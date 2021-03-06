package edu.ucdenver.ccp.PhenoGen.tools.analysis;

import edu.ucdenver.ccp.PhenoGen.driver.RException;
import edu.ucdenver.ccp.PhenoGen.driver.R_session;
import edu.ucdenver.ccp.PhenoGen.data.AsyncUpdateDataset;
import edu.ucdenver.ccp.PhenoGen.data.Dataset;
import edu.ucdenver.ccp.PhenoGen.data.User;
import edu.ucdenver.ccp.PhenoGen.data.Bio.Gene;
import edu.ucdenver.ccp.PhenoGen.data.Bio.BQTL;
import edu.ucdenver.ccp.PhenoGen.data.Bio.EQTL;
import edu.ucdenver.ccp.PhenoGen.data.Bio.Transcript;
import edu.ucdenver.ccp.PhenoGen.data.Bio.TranscriptCluster;
import edu.ucdenver.ccp.PhenoGen.data.Bio.SmallNonCodingRNA;
import edu.ucdenver.ccp.PhenoGen.data.Bio.Annotation;
import edu.ucdenver.ccp.PhenoGen.data.Bio.RNASequence;
import edu.ucdenver.ccp.PhenoGen.data.Bio.SequenceVariant;
import edu.ucdenver.ccp.PhenoGen.driver.PerlHandler;
import edu.ucdenver.ccp.PhenoGen.driver.PerlException;
import edu.ucdenver.ccp.PhenoGen.driver.ExecHandler;
import edu.ucdenver.ccp.PhenoGen.driver.ExecException;
import edu.ucdenver.ccp.util.FileHandler;
import edu.ucdenver.ccp.util.ObjectHandler;
import edu.ucdenver.ccp.PhenoGen.tools.analysis.Statistic;
import edu.ucdenver.ccp.PhenoGen.tools.analysis.AsyncGeneDataExpr;
import edu.ucdenver.ccp.PhenoGen.tools.analysis.AsyncGeneDataTools;



import java.util.GregorianCalendar;
import java.util.Date;

import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.ucdenver.ccp.PhenoGen.web.mail.*;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import javax.sql.DataSource;





public class GeneDataTools {
    private ArrayList<Thread> threadList;
    private String[] rErrorMsg = null;
    private R_session myR_session = new R_session();
    //private PerlHandler myPerl_session=null;
    private ExecHandler myExec_session = null;
    private HttpSession session = null;
    private User userLoggedIn = null;
    private DataSource pool = null;
    private Logger log = null;
    private String perlDir = "", fullPath = "";
    private String rFunctDir = "";
    private String userFilesRoot = "";
    private String urlPrefix = "";
    private int validTime=7*24*60*60*1000;
    private String perlEnvVar="";
    private String ucscDir="";
    private String ucscGeneDir="";
    private String bedDir="";
    private String geneSymbol="";
    private String ucscURL="";
    private String deMeanURL="";
    private String deFoldDiffURL="";
    private String chrom="";
    private String dbPropertiesFile="";
    private String ensemblDBPropertiesFile="";
    private String ucscDBVerPropertiesFile="";
    private String mongoDBPropertiesFile="";
    private int minCoord=0;
    private int maxCoord=0;
    FileHandler myFH=new FileHandler();
    private int usageID=-1;
    private int maxThreadRunning=1;
    String outputDir="";
    private boolean pathReady=false;
    
    private String  returnGenURL="";
    private String  returnUCSCURL= "";
    private String  returnOutputDir="";
    private String returnGeneSymbol="";

    private String insertUsage="insert into TRANS_DETAIL_USAGE (INPUT_ID,IDECODER_RESULT,RUN_DATE,ORGANISM) values (?,?,?,?)";
    String updateSQL="update TRANS_DETAIL_USAGE set TIME_TO_RETURN=? , RESULT=? where TRANS_DETAIL_ID=?";
    private HashMap eQTLRegions=new HashMap();
    //HashMap<String,HashMap> cacheHM=new HashMap<String,HashMap>();
    //ArrayList<String> cacheList=new ArrayList<String>();
    int maxCacheList=5;

    
    

    public GeneDataTools() {
        log = Logger.getRootLogger();
    }
    
    public boolean isPathReady(){
        return this.pathReady;
    }
    
    public void resetPathReady(){
        this.pathReady=false;
    }
    
    public int[] getOrganismSpecificIdentifiers(String organism,String genomeVer){
        
            int[] ret=new int[2];
            String organismLong="Mouse";
            if(organism.equals("Rn")){
                organismLong="Rat";
            }
            String atQuery="select Array_type_id from array_types "+
                        "where array_name like 'Affymetrix GeneChip "+organismLong+" Exon 1.0 ST Array'";
            
            /*
            *  This does only look for the brain RNA dataset id.  Right now the tables link that RNA Dataset ID to
            *  the other datasets.  This means finding the right organism and genome version for now is sufficient without
            *  regard to tissues as all other tables link to the brain dataset since we have brain for both supported organisms
            */
            String rnaIDQuery="select rna_dataset_id from RNA_DATASET "+
                        "where organism = '"+organism+"' and tissue='Brain' and strain_panel='BNLX/SHRH' and visible=1 and genome_id='"+genomeVer+"'";
            Connection conn=null;
            PreparedStatement ps=null;
            try {
                conn=pool.getConnection();
                ps = conn.prepareStatement(atQuery);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    ret[0]=rs.getInt(1);
                }
                ps.close();
            } catch (SQLException ex) {
                log.error("SQL Exception retreiving Array_Type_ID from array_types for Organism="+organism ,ex);
                try {
                    ps.close();
                } catch (Exception ex1) {
                   
                }
            }
            try {
                if(conn==null || conn.isClosed()){
                    conn=pool.getConnection();
                }
                ps = conn.prepareStatement(rnaIDQuery);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    ret[1]=rs.getInt(1);
                }
                ps.close();
                conn.close();
            } catch (SQLException ex) {
                log.error("SQL Exception retreiving RNA_dataset_ID from RNA_DATASET for Organism="+organism ,ex);
                try {
                    ps.close();
                } catch (Exception ex1) {

                }
            }finally{
                    try {
                            if(conn!=null)
                                conn.close();
                        } catch (SQLException ex) {
                        }
            }
            return ret;
        
    }
    
    public int[] getOrganismSpecificIdentifiers(String organism,String tissue,String genomeVer){
        
            int[] ret=new int[2];
            String organismLong="Mouse";
            if(organism.equals("Rn")){
                organismLong="Rat";
            }
            if(tissue.equals("Whole Brain")){
                tissue="Brain";
            }
            String atQuery="select Array_type_id from array_types "+
                        "where array_name like 'Affymetrix GeneChip "+organismLong+" Exon 1.0 ST Array'";
            
            /*
            *  This does only look for the brain RNA dataset id.  Right now the tables link that RNA Dataset ID to
            *  the other datasets.  This means finding the right organism and genome version for now is sufficient without
            *  regard to tissues as all other tables link to the brain dataset since we have brain for both supported organisms
            */
            String rnaIDQuery="select rna_dataset_id from RNA_DATASET "+
                        "where organism = '"+organism+"' and tissue='"+tissue+"' and strain_panel='BNLX/SHRH' and visible=1 and genome_id='"+genomeVer+"'";
            log.debug("\nRNAID Query:\n"+rnaIDQuery);
            Connection conn=null;
            PreparedStatement ps=null;
            try {
                conn=pool.getConnection();
                ps = conn.prepareStatement(atQuery);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    ret[0]=rs.getInt(1);
                }
                ps.close();
            } catch (SQLException ex) {
                log.error("SQL Exception retreiving Array_Type_ID from array_types for Organism="+organism ,ex);
                try {
                    ps.close();
                } catch (Exception ex1) {
                   
                }
            }
            try {
                if(conn==null || conn.isClosed()){
                    conn=pool.getConnection();
                }
                ps = conn.prepareStatement(rnaIDQuery);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    ret[1]=rs.getInt(1);
                }
                ps.close();
                conn.close();
            } catch (SQLException ex) {
                log.error("SQL Exception retreiving RNA_dataset_ID from RNA_DATASET for Organism="+organism ,ex);
                try {
                    ps.close();
                } catch (Exception ex1) {

                }
            }finally{
                    try {
                            if(conn!=null)
                                conn.close();
                        } catch (SQLException ex) {
                        }
            }
            return ret;
    }
    
    public HashMap<String,String> getGenomeVersionSource(String genomeVer){
        
            HashMap<String,String> hm=new HashMap<String,String>();
            String query="select * from Browser_Genome_versions "+
                        "where genome_id='"+genomeVer+"'";

            Connection conn=null;
            PreparedStatement ps=null;
            try {
                conn=pool.getConnection();
                ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                if(rs.next()){
                    hm.put("ensembl",rs.getString("ENSEMBL"));
                    hm.put("ucsc",rs.getString("UCSC"));
                }
                ps.close();
                conn.close();
                conn=null;
            } catch (SQLException ex) {
                log.error("SQL Exception retreiving datasources for genome Version="+genomeVer ,ex);
                try {
                   if(conn!=null && !conn.isClosed()){
                       try{
                           conn.close();
                           conn=null;
                       }catch(SQLException e){}
                   }
                } catch (Exception ex1) {
                   
                }
            }
            
            return hm;
        
    }

    
    public String getGeneFolder(String inputID,String ensemblIDList,
            String panel,String organism,String genomeVer,int RNADatasetID,int arrayTypeID) {
        String ret="";
        String[] ensemblList = ensemblIDList.split(",");
        String ensemblID1 = ensemblList[0];
        boolean error=false;
        if(ensemblID1!=null && !ensemblID1.equals("")){
            //Define output directory
            String tmpoutputDir = fullPath + "tmpData/browserCache/"+genomeVer+"/geneData/" + ensemblID1 + "/";
            //session.setAttribute("geneCentricPath", outputDir);
            log.debug("checking for path:"+tmpoutputDir);
            String folderName = ensemblID1;
            //String publicPath = H5File.substring(H5File.indexOf("/Datasets/") + 10);
            //publicPath = publicPath.substring(0, publicPath.indexOf("/Affy.NormVer.h5"));
            
           
        String[] loc=null;
        try{
                loc=myFH.getFileContents(new File(tmpoutputDir+"location.txt"));
        }catch(IOException e){
                log.error("Couldn't load location for gene.",e);
        }
        if(loc!=null){
                chrom=loc[0];
                minCoord=Integer.parseInt(loc[1]);
                maxCoord=Integer.parseInt(loc[2]);
        }
        //log.debug("getGeneCentricData->getRegionData");
        ret=this.getImageRegionData(chrom, minCoord, maxCoord, panel, organism,genomeVer, RNADatasetID, arrayTypeID, 0.001,false);
        }else{
            ret="";
        }
        return ret;
    }
    /**
     * Calls the Perl script WriteXML_RNA.pl and R script ExonCorrelation.R.
     * @param ensemblID       the ensemblIDs as a comma separated list
     * @param panel 
     * @param organism        the organism         
     * 
     */
    public ArrayList<Gene> getGeneCentricData(String inputID,String ensemblIDList,
            String panel,String organism,String genomeVer,int RNADatasetID,int arrayTypeID,boolean eQTL) {
        
        //Setup a String in the format YYYYMMDDHHMM to append to the folder
        Date start = new Date();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(start);
        String rOutputPath = "";
        outputDir="";
        String result="";
        returnGenURL="";
        HashMap<String,String> source=this.getGenomeVersionSource(genomeVer);
        

        try(Connection conn=pool.getConnection()){
            PreparedStatement ps=conn.prepareStatement(insertUsage, PreparedStatement.RETURN_GENERATED_KEYS);
            //ps.setInt(1, usageID);
            ps.setString(1,inputID);
            ps.setString(2, ensemblIDList);
            ps.setTimestamp(3, new Timestamp(start.getTime()));
            ps.setString(4, organism);
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                usageID = rs.getInt(1);
            }
            ps.close();

        }catch(SQLException e){
            log.error("Error saving Transcription Detail Usage",e);
        }
        Date endDBSetup=new Date();
        //EnsemblIDList can be a comma separated list break up the list
        String[] ensemblList = ensemblIDList.split(",");
        String ensemblID1 = ensemblList[0];
        boolean error=false;
        if(ensemblID1!=null && !ensemblID1.equals("")){
            //Define output directory
            outputDir = fullPath + "tmpData/browserCache/"+genomeVer+"/geneData/" + ensemblID1 + "/";
            //session.setAttribute("geneCentricPath", outputDir);
            log.debug("checking for path:"+outputDir);
            String folderName = ensemblID1;
            //String publicPath = H5File.substring(H5File.indexOf("/Datasets/") + 10);
            //publicPath = publicPath.substring(0, publicPath.indexOf("/Affy.NormVer.h5"));
            
            try {
                File geneDir=new File(outputDir);
                File errorFile=new File(outputDir+"errMsg.txt");
                if(geneDir.exists()){
                    Date lastMod=new Date(geneDir.lastModified());
                    Date prev2Months=new Date(start.getTime()-(60*24*60*60*1000));
                    if(lastMod.before(prev2Months)||errorFile.exists()){
                        if(myFH.deleteAllFilesPlusDirectory(geneDir)) {
                        }
                        error=generateFiles(organism,genomeVer,source.get("ensembl"),rOutputPath,ensemblIDList,folderName,ensemblID1,RNADatasetID,arrayTypeID,panel);
                        result="old files, regenerated all files";

                    }else{
                        //do nothing just need to set session var
                        String errors;
                        errors = loadErrorMessage();
                        if(errors.equals("")){
                            //String[] results=this.createImage("probe,numExonPlus,numExonMinus,noncoding,smallnc,refseq", organism,outputDir,chrom,minCoord,maxCoord);
                            //getUCSCUrl(results[1].replaceFirst(".png", ".url"));
                            //getUCSCUrls(ensemblID1);
                            result="cache hit files not generated";
                        }else{
                            if(myFH.deleteAllFilesPlusDirectory(geneDir)) {
                            }
                            error=generateFiles(organism,genomeVer,source.get("ensembl"),rOutputPath,ensemblIDList,folderName,ensemblID1,RNADatasetID,arrayTypeID,panel);
                            result="old files, regenerated all files";

                        }
                    }
                }else{
                    error=generateFiles(organism,genomeVer,source.get("ensembl"),rOutputPath,ensemblIDList,folderName,ensemblID1,RNADatasetID,arrayTypeID,panel);
                    if(!error){
                        result="NewGene generated successfully";
                    }
                }
                
            } catch (Exception e) {
                error=true;
                
                log.error("In Exception getting Gene Centric Results", e);
                Email myAdminEmail = new Email();
                String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
                myAdminEmail.setSubject("Exception thrown getting Gene Centric Results");
                myAdminEmail.setContent("There was an error while getting gene centric results.\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                       
                        //throw new RuntimeException();
                    
                    }
                }
            }
        }else{
            error=true;
            setError("No Ensembl IDs");
        }
        Date endFindGen=new Date();
        if(error){
            result=(String)session.getAttribute("genURL");
        }
        this.setPublicVariables(error,genomeVer,ensemblID1);
        Date endLoadLoc=new Date();
        Date endRegion=new Date();
        ArrayList<Gene> ret=new ArrayList<Gene>();
        if(!error){
            String[] loc=null;
            try{
                    loc=myFH.getFileContents(new File(outputDir+"location.txt"));
            }catch(IOException e){
                    log.error("Couldn't load location for gene.",e);
            }
            if(loc!=null){
                    chrom=loc[0];
                    minCoord=Integer.parseInt(loc[1]);
                    maxCoord=Integer.parseInt(loc[2]);
            }
            endLoadLoc=new Date();
            //log.debug("getGeneCentricData->getRegionData");
            if(!chrom.toLowerCase().startsWith("chr")){
                chrom="chr"+chrom;
            }
            ret=this.getRegionData(chrom, minCoord, maxCoord, panel, organism,genomeVer, RNADatasetID, arrayTypeID, 0.01,eQTL);
            for(int i=0;i<ret.size();i++){
                //log.debug(ret.get(i).getGeneID()+"::"+ensemblIDList);
                if(ret.get(i).getGeneID().equals(ensemblIDList)){
                    //log.debug("EQUAL::"+ret.get(i).getGeneID()+"::"+ensemblIDList);
                    this.returnGeneSymbol=ret.get(i).getGeneSymbol();
                }
            }
            endRegion=new Date();
        }
        try(Connection conn=pool.getConnection()){
            PreparedStatement ps=conn.prepareStatement(updateSQL, 
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_UPDATABLE);
            Date end=new Date();
            long returnTimeMS=end.getTime()-start.getTime();
            ps.setLong(1, returnTimeMS);
            ps.setString(2, result);
            ps.setInt(3, usageID);
            ps.executeUpdate();
            ps.close();
        }catch(SQLException e){
            log.error("Error saving Transcription Detail Usage",e);
        }
        Date endDB=new Date();
        
        log.debug("Timing:");
        log.debug("Total:"+(endDB.getTime()-start.getTime())/1000+"s");
        log.debug("DB Setup:"+(endDBSetup.getTime()-start.getTime())/1000+"s");
        log.debug("Find Gene:"+(endFindGen.getTime()-endDBSetup.getTime())/1000+"s");
        log.debug("Load Location:"+(endLoadLoc.getTime()-endFindGen.getTime())/1000+"s");
        log.debug("Get Region:"+(endRegion.getTime()-endLoadLoc.getTime())/1000+"s");
        log.debug("DB Final:"+(endDB.getTime()-endRegion.getTime())/1000+"s");
        return ret;
    }
    
    public HashMap<String,Integer> getRegionTrackList(String chromosome,int min,int max,String panel,String myOrganism,String genomeVer,int rnaDatasetID,int arrayTypeID,String track){
        HashMap<String,Integer> ret=new HashMap<String,Integer>();
        chromosome=chromosome.toLowerCase();
        if(!chromosome.startsWith("chr")){
            chromosome="chr"+chromosome;
        }
        
        //Setup a String in the format YYYYMMDDHHMM to append to the folder
        Date start = new Date();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(start);
        String datePart=Integer.toString(gc.get(gc.MONTH)+1)+
                Integer.toString(gc.get(gc.DAY_OF_MONTH))+
                Integer.toString(gc.get(gc.YEAR))+"_"+
                Integer.toString(gc.get(gc.HOUR_OF_DAY))+
                Integer.toString(gc.get(gc.MINUTE))+
                Integer.toString(gc.get(gc.SECOND));
        
        HashMap<String,String> source=this.getGenomeVersionSource(genomeVer);
        
        
        //EnsemblIDList can be a comma separated list break up the list
        boolean error=false;

            //Define output directory
            outputDir = fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/" +myOrganism+ chromosome+"_"+minCoord+"_"+maxCoord+"_"+datePart + "/";
            //session.setAttribute("geneCentricPath", outputDir);
            log.debug("checking for path:"+outputDir);
            String folderName = myOrganism+chromosome+"_"+minCoord+"_"+maxCoord+"_"+datePart;
            //String publicPath = H5File.substring(H5File.indexOf("/Datasets/") + 10);
            //publicPath = publicPath.substring(0, publicPath.indexOf("/Affy.NormVer.h5"));
            RegionDirFilter rdf=new RegionDirFilter(myOrganism+ chromosome+"_"+minCoord+"_"+maxCoord+"_");
            File mainDir=new File(fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/");
            File[] list=mainDir.listFiles(rdf);
            try {
                File geneDir=new File(outputDir);
                File errorFile=new File(outputDir+"errMsg.txt");
                
                if(list.length>0){
                    outputDir=list[0].getAbsolutePath()+"/";
                    int second=outputDir.lastIndexOf("/",outputDir.length()-2);
                    folderName=outputDir.substring(second+1,outputDir.length()-1);
                    String errors;
                    errors = loadErrorMessage();
                    if(errors.equals("")){
                        
                    }else{
                        //ERROR
                    }
                }else{
                    //ERROR
                }
                
                
                
            } catch (Exception e) {
                error=true;
                log.error("In Exception getting Gene List for a track", e);
                Email myAdminEmail = new Email();
                String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
                myAdminEmail.setSubject("Exception thrown getting Gene List for a track");
                myAdminEmail.setContent("There was an error while getting Gene List for a track.\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }
        
        this.pathReady=true;
        
        ret=Gene.readGeneIDList(outputDir+track+".xml");
        log.debug("getRegionData() returning gene list of size:"+ret.size());
        return ret;
    }
    
    public ArrayList<Gene> getMergedRegionData(String chromosome,int minCoord,int maxCoord,
            String panel,
            String organism,String genomeVer,int RNADatasetID,int arrayTypeID,double pValue,boolean withEQTL) {
        return this.getRegionDataMain(chromosome,minCoord,maxCoord,panel,organism,genomeVer,RNADatasetID,arrayTypeID,pValue,withEQTL,"mergedTotal.xml");
    }
    
    public ArrayList<Gene> getRegionData(String chromosome,int minCoord,int maxCoord,
            String panel,
            String organism,String genomeVer,int RNADatasetID,int arrayTypeID,double pValue,boolean withEQTL) {
        return this.getRegionDataMain(chromosome,minCoord,maxCoord,panel,organism,genomeVer,RNADatasetID,arrayTypeID,pValue,withEQTL,"Region.xml");
    }
    
    public ArrayList<Gene> getRegionDataMain(String chromosome,int minCoord,int maxCoord,
            String panel,
            String organism,String genomeVer,int RNADatasetID,int arrayTypeID,double pValue,boolean withEQTL,String file) {
        
        
        chromosome=chromosome.toLowerCase();
        if(!chromosome.startsWith("chr")){
            chromosome="chr"+chromosome;
        }
        
        //Setup a String in the format YYYYMMDDHHMM to append to the folder
        Date start = new Date();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(start);
        String datePart=Integer.toString(gc.get(gc.MONTH)+1)+
                Integer.toString(gc.get(gc.DAY_OF_MONTH))+
                Integer.toString(gc.get(gc.YEAR))+"_"+
                Integer.toString(gc.get(gc.HOUR_OF_DAY))+
                Integer.toString(gc.get(gc.MINUTE))+
                Integer.toString(gc.get(gc.SECOND));
        String rOutputPath = "";
        outputDir="";
        String result="";
        this.minCoord=minCoord;
        this.maxCoord=maxCoord;
        this.chrom=chromosome;
        String inputID=organism+":"+chromosome+":"+minCoord+"-"+maxCoord;
        HashMap<String,String> source=this.getGenomeVersionSource(genomeVer);

        try(Connection conn=pool.getConnection()){

            PreparedStatement ps=conn.prepareStatement(insertUsage, PreparedStatement.RETURN_GENERATED_KEYS);
            //ps.setInt(1, usageID);
            ps.setString(1,inputID);
            ps.setString(2, "");
            ps.setTimestamp(3, new Timestamp(start.getTime()));
            ps.setString(4, organism);
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                usageID = rs.getInt(1);
            }
            ps.close();
        }catch(SQLException e){
            log.error("Error saving Transcription Detail Usage",e);
        }
        
        //EnsemblIDList can be a comma separated list break up the list
        boolean error=false;

            //Define output directory
            outputDir = fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/" +organism+ chromosome+"_"+minCoord+"_"+maxCoord+"_"+datePart + "/";
            //session.setAttribute("geneCentricPath", outputDir);
            log.debug("checking for path:"+outputDir);
            String folderName = organism+chromosome+"_"+minCoord+"_"+maxCoord+"_"+datePart;
            //String publicPath = H5File.substring(H5File.indexOf("/Datasets/") + 10);
            //publicPath = publicPath.substring(0, publicPath.indexOf("/Affy.NormVer.h5"));
            RegionDirFilter rdf=new RegionDirFilter(organism+ chromosome+"_"+minCoord+"_"+maxCoord+"_");
            File mainDir=new File(fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/");
            File[] list=mainDir.listFiles(rdf);
            try {
                File geneDir=new File(outputDir);
                File errorFile=new File(outputDir+"errMsg.txt");
                if(geneDir.exists()){
                        //do nothing just need to set session var
                        String errors;
                        errors = loadErrorMessage();
                        if(errors.equals("")){
                            //String[] results=this.createImage("default", organism,outputDir,chrom,minCoord,maxCoord);
                            //getUCSCUrl(results[1].replaceFirst(".png", ".url"));
                            result="cache hit files not generated";
                            
                        }else{
                            result="Previous Result had errors. Trying again.";
                            generateRegionFiles(organism,genomeVer,source.get("ensembl"),folderName,RNADatasetID,arrayTypeID,source.get("ucsc"));
                            
                            //error=true;
                            //this.setError(errors);
                        }
                }else{
                    if(list.length>0){
                        outputDir=list[0].getAbsolutePath()+"/";
                        int second=outputDir.lastIndexOf("/",outputDir.length()-2);
                        folderName=outputDir.substring(second+1,outputDir.length()-1);
                        String errors;
                        errors = loadErrorMessage();
                        if(errors.equals("")){
                            //String[] results=this.createImage("default", organism,outputDir,chrom,minCoord,maxCoord);
                            //getUCSCUrl(results[1].replaceFirst(".png", ".url"));
                            result="cache hit files not generated";
                        }else{
                            result="Previous Result had errors. Trying again.";
                            generateRegionFiles(organism,genomeVer,source.get("ensembl"),folderName,RNADatasetID,arrayTypeID,source.get("ucsc"));
                            
                            //error=true;
                            //this.setError(errors);
                        }
                    }else{
                        generateRegionFiles(organism,genomeVer,source.get("ensembl"),folderName,RNADatasetID,arrayTypeID,source.get("ucsc"));
                        result="New Region generated successfully";
                    }
                }
                
                
            } catch (Exception e) {
                error=true;
                
                log.error("In Exception getting Gene Centric Results", e);
                Email myAdminEmail = new Email();
                String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
                myAdminEmail.setSubject("Exception thrown getting Gene Centric Results");
                myAdminEmail.setContent("There was an error while getting gene centric results.\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }
        if(error){
            result=this.returnGenURL;
        }
        this.setPublicVariables(error,genomeVer,folderName);
        this.pathReady=true;
        
        ArrayList<Gene> ret=Gene.readGenes(outputDir+file);
        log.debug("getRegionData() returning gene list of size:"+ret.size());

        if(withEQTL){
            this.addHeritDABG(ret,minCoord,maxCoord,organism,chromosome,RNADatasetID, arrayTypeID,genomeVer);
            ArrayList<TranscriptCluster> tcList=getTransControlledFromEQTLs(minCoord,maxCoord,chromosome,arrayTypeID,pValue,"All");
            HashMap<String,TranscriptCluster> transInQTLsCore=new HashMap<String,TranscriptCluster>();
            HashMap<String,TranscriptCluster> transInQTLsExtended=new HashMap<String,TranscriptCluster>();
            HashMap<String,TranscriptCluster> transInQTLsFull=new HashMap<String,TranscriptCluster>();
            for(int i=0;i<tcList.size();i++){
                TranscriptCluster tmp=tcList.get(i);
                if(tmp.getLevel().equals("core")){
                    transInQTLsCore.put(tmp.getTranscriptClusterID(),tmp);
                }else if(tmp.getLevel().equals("extended")){
                    transInQTLsExtended.put(tmp.getTranscriptClusterID(),tmp);
                }else if(tmp.getLevel().equals("full")){
                    transInQTLsFull.put(tmp.getTranscriptClusterID(),tmp);
                }
            }
            addFromQTLS(ret,transInQTLsCore,transInQTLsExtended,transInQTLsFull);
        }

        try(Connection conn=pool.getConnection()){
            PreparedStatement ps=conn.prepareStatement(updateSQL, 
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_UPDATABLE);
            Date end=new Date();
            long returnTimeMS=end.getTime()-start.getTime();
            ps.setLong(1, returnTimeMS);
            ps.setString(2, result);
            ps.setInt(3, usageID);
            ps.executeUpdate();
            ps.close();
        }catch(SQLException e){
            log.error("Error saving Transcription Detail Usage",e);
        }
        return ret;
    }
    
    public String getImageRegionData(String chromosome,int minCoord,int maxCoord,
            String panel,String organism,String genomeVer,int RNADatasetID,int arrayTypeID,double pValue,boolean img) {
        
        
        chromosome=chromosome.toLowerCase();
        if(!chromosome.startsWith("chr")){
            chromosome="chr"+chromosome;
        }
        //Setup a String in the format YYYYMMDDHHMM to append to the folder
        Date start = new Date();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(start);
        String datePart=Integer.toString(gc.get(gc.MONTH)+1)+
                Integer.toString(gc.get(gc.DAY_OF_MONTH))+
                Integer.toString(gc.get(gc.YEAR))+"_"+
                Integer.toString(gc.get(gc.HOUR_OF_DAY))+
                Integer.toString(gc.get(gc.MINUTE))+
                Integer.toString(gc.get(gc.SECOND));
        String rOutputPath = "";
        outputDir="";
        String result="";
        this.minCoord=minCoord;
        this.maxCoord=maxCoord;
        this.chrom=chromosome;
        String inputID=organism+":"+chromosome+":"+minCoord+"-"+maxCoord;
        String imgStr="img";
        if(!img){
            imgStr="";
        }
        HashMap<String,String> source=this.getGenomeVersionSource(genomeVer);
        //EnsemblIDList can be a comma separated list break up the list
        boolean error=false;

            //Define output directory
            outputDir = fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/"+imgStr +organism+ chromosome+"_"+minCoord+"_"+maxCoord+"_"+datePart + "/";
            //session.setAttribute("geneCentricPath", outputDir);
            log.debug("checking for path:"+outputDir);
            String folderName = imgStr +organism+chromosome+"_"+minCoord+"_"+maxCoord+"_"+datePart;
            //String publicPath = H5File.substring(H5File.indexOf("/Datasets/") + 10);
            //publicPath = publicPath.substring(0, publicPath.indexOf("/Affy.NormVer.h5"));
            RegionDirFilter rdf=new RegionDirFilter(imgStr+organism+ chromosome+"_"+minCoord+"_"+maxCoord+"_");
            File mainDir=new File(fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/");
            File[] list=mainDir.listFiles(rdf);
            try {
                File geneDir=new File(outputDir);
                File errorFile=new File(outputDir+"errMsg.txt");
                if(geneDir.exists()){
                        //do nothing just need to set session var
                        String errors;
                        errors = loadErrorMessage();
                        if(errors.equals("")){
                            result="cache hit files not generated";
                            
                        }else{
                            result="Previous Result had errors. Trying again.";
                            generateRegionFiles(organism,genomeVer,source.get("ensembl"),folderName,RNADatasetID,arrayTypeID,source.get("ucsc"));
                        }
                }else{
                    if(list.length>0){
                        
                        outputDir=list[0].getAbsolutePath()+"/";
                        int second=outputDir.lastIndexOf("/",outputDir.length()-2);
                        folderName=outputDir.substring(second+1,outputDir.length()-1);
                        log.debug("previous exists:"+outputDir);
                        log.debug("set folder:"+folderName);
                        String errors;
                        errors = loadErrorMessage();
                        if(errors.equals("")){
                            result="cache hit files not generated";
                        }else{
                            result="Previous Result had errors. Trying again.";
                            generateRegionFiles(organism,genomeVer,source.get("ensembl"),folderName,RNADatasetID,arrayTypeID,source.get("ucsc"));
                        }
                    }else{
                        generateRegionFiles(organism,genomeVer,source.get("ensembl"),folderName,RNADatasetID,arrayTypeID,source.get("ucsc"));
                        result="New Region generated successfully";
                    }
                }
                
                
            } catch (Exception e) {
                error=true;
                
                log.error("In Exception getting Gene Centric Results", e);
                Email myAdminEmail = new Email();
                String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
                myAdminEmail.setSubject("Exception thrown getting Gene Centric Results");
                myAdminEmail.setContent("There was an error while getting gene centric results.\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }
        if(error){
            result=(String)session.getAttribute("genURL");
        }
        return outputDir;
    }

    public boolean generateFiles(String organism,String genomeVer,String ensemblPath,String rOutputPath, String ensemblIDList,String folderName,String ensemblID1,int RNADatasetID,int arrayTypeID,String panel) {
        log.debug("generate files");
        AsyncGeneDataTools prevThread=null;
        boolean error = false;
        log.debug("outputDir:"+outputDir);
        File outDirF = new File(outputDir);
        //Mkdir if some are missing    
        if (!outDirF.exists()) {
            outDirF.mkdirs();
        }
        
        boolean createdXML=this.createXMLFiles(organism,genomeVer,ensemblIDList,ensemblID1,ensemblPath);
        
        log.debug(ensemblIDList+" CreatedXML::"+createdXML);
        
        if(createdXML){
            String[] loc=null;
            try{
                loc=myFH.getFileContents(new File(outputDir+"location.txt"));
            }catch(IOException e){
                error=true;
                log.error("Couldn't load location for gene.",e);
            }
            if(loc!=null){
                chrom=loc[0];
                minCoord=Integer.parseInt(loc[1]);
                maxCoord=Integer.parseInt(loc[2]);
                log.debug("AsyncGeneDataTools with "+chrom+":"+minCoord+"-"+maxCoord);
                callWriteXML(ensemblID1,organism,genomeVer,chrom, minCoord, maxCoord,arrayTypeID,RNADatasetID);
                boolean isENS=false;
                if(ensemblID1.startsWith("ENS")){
                    isENS=true;
                }
                prevThread=callAsyncGeneDataTools(chrom, minCoord, maxCoord,arrayTypeID,RNADatasetID,genomeVer,isENS);
            }
        }else{
            error=true;
        }    
        
        return error;
    }
    
    public boolean generateGeneRegionFiles(String organism,String genomeVer,String folderName,int RNADatasetID,int arrayTypeID) {
        log.debug("generate files");
        log.debug("outputDir:"+outputDir);
        File outDirF = new File(outputDir);
        //Mkdir if some are missing    
        if (!outDirF.exists()) {
            outDirF.mkdirs();
        }
        HashMap<String,String> source=this.getGenomeVersionSource(genomeVer);
        String ensemblPath=source.get("ensembl");
        //boolean createdXML=this.createRegionImagesXMLFiles(folderName,organism,genomeVer,ensemblPath,arrayTypeID,RNADatasetID,source.get("ucsc"));
        AsyncBrowserRegion abr=new AsyncBrowserRegion(session,pool,organism,outputDir,chrom,minCoord,maxCoord,arrayTypeID,RNADatasetID,genomeVer,source.get("ucsc"),ensemblPath,usageID,false);
        abr.start();
        return true;
    }
    
    public boolean generateRegionFiles(String organism,String genomeVer,String ensemblPath,String folderName,int RNADatasetID,int arrayTypeID,String ucscDB) {
        log.debug("generate files");
        boolean completedSuccessfully = false;
        log.debug("outputDir:"+outputDir);
        File outDirF = new File(outputDir);
        //Mkdir if some are missing    
        if (!outDirF.exists()) {
            //log.debug("make output dir");
            outDirF.mkdirs();
        }
        AsyncBrowserRegion abr=new AsyncBrowserRegion(session,pool,organism,outputDir,chrom,minCoord,maxCoord,arrayTypeID,RNADatasetID,genomeVer,ucscDB,ensemblPath,usageID,true);
        abr.start();
        //boolean createdXML=this.createRegionImagesXMLFiles(folderName,organism,genomeVer,ensemblPath,arrayTypeID,RNADatasetID,ucscDB);
        //AsyncGeneDataTools prevThread=callAsyncGeneDataTools(chrom, minCoord, maxCoord,arrayTypeID,RNADatasetID,genomeVer,false);
        return true;
    }
    
    public ArrayList<String> getPhenoGenID(String ensemblID,String genomeVer) throws SQLException{
        Connection conn=null;
        ArrayList<String> ret=new ArrayList<String>();
        try{
           conn=pool.getConnection();
           String org="Rn";
           if(ensemblID.startsWith("ENSMMU")){
               org="Mm";
           }
           String query="select rt.gene_id,rta.annotation from rna_transcripts_annot rta, rna_transcripts rt "+
                        "where rt.RNA_TRANSCRIPT_ID=rta.RNA_TRANSCRIPT_ID "+
                        "and rt.RNA_DATASET_ID=? "+
                        "and rta.ANNOTATION like '"+ensemblID+"%'";
           int[] tmp=getOrganismSpecificIdentifiers(org,"Merged",genomeVer);
           int dsid=tmp[1];
           PreparedStatement ps=conn.prepareStatement(query);
           ps.setInt(1, dsid);
           ResultSet rs=ps.executeQuery();
           
           while(rs.next()){
               String id=rs.getString(1);
               if(id.startsWith("Merged_GPRN")){
                   id=id.substring(8);
               }
               boolean found=false;
               for(int i=0;i<ret.size()&&!found;i++){
                   if(ret.get(i).equals(id)){
                       found=true;
                   }
               }
               if(!found){
                   ret.add(id);
               }
           }
           ps.close();
           conn.close();
        }catch(SQLException e){
            try{
                if(conn!=null && !conn.isClosed()){
                    conn.close();
                    conn=null;
                }
            }catch(SQLException ex){}
            throw(e);
        }
        return ret;
    }
    
    private void outputProbesetIDFiles(String outputDir,String chr, int min, int max,int arrayTypeID,int rnaDS_ID,String genomeVer){
        if(chr.toLowerCase().startsWith("chr")){
            chr=chr.substring(3);
        }
        String probeQuery="select s.Probeset_ID "+
                                "from Chromosomes c, Affy_Exon_ProbeSet s "+
                                "where s.chromosome_id = c.chromosome_id "+
                                "and c.name = '"+chr.toUpperCase()+"' "+
                            "and ( "+
                            "(s.psstart >= "+min+" and s.psstart <="+max+") OR "+
                            "(s.psstop >= "+min+" and s.psstop <= "+max+") OR "+
                            "(s.psstart <= "+min+" and s.psstop >="+min+")"+
                            ") "+
                            "and s.psannotation <> 'transcript' " +
                            "and s.updatedlocation = 'Y' "+
                            "and s.Array_TYPE_ID = "+arrayTypeID;

        String probeTransQuery="select distinct s.Probeset_ID,c2.name,s.PSSTART,s.PSSTOP,s.PSLEVEL,s.Strand "+
                "from location_specific_eqtl l "+
                "left outer join snps sn on sn.snp_id=l.SNP_ID "+
                "left outer join Affy_Exon_ProbeSet s on s.probeset_id = l.probe_id "+
                "left outer join Chromosomes c2 on c2.chromosome_id = s.chromosome_id "+
                "where sn.genome_id='"+genomeVer+"' "+
                "and c2.name = '"+chr.toUpperCase()+"' "+
                "and s.genome_id='"+genomeVer+"' "+
                "and ( "+
                "(s.psstart >= "+min+" and s.psstart <="+max+") OR "+
                "(s.psstop >= "+min+" and s.psstop <= "+max+") OR "+
                "(s.psstart <= "+min+" and s.psstop >="+min+") ) "+
                "and s.psannotation = 'transcript' " +
                "and s.updatedlocation = 'Y' "+
                "and s.Array_TYPE_ID = " + arrayTypeID +" )";


        /*String probeTransQuery="select distinct s.Probeset_ID,c2.name,s.PSSTART,s.PSSTOP,s.PSLEVEL,s.Strand "+
                "from location_specific_eqtl l "+
                "left outer join snps sn on sn.snp_id=l.SNP_ID "+
                "left outer join Affy_Exon_ProbeSet s on s.probeset_id = l.probe_id "+
                "left outer join Chromosomes c2 on c2.chromosome_id = s.chromosome_id "+
                "where sn.genome_id='"+genomeVer+"' "+
                "and l.probe_id in (select distinct ae.Probeset_ID " +
                "from Affy_Exon_ProbeSet ae "+
                "left outer join Chromosomes c on c.chromosome_id = ae.chromosome_id "+
                "where c.name = '"+chr.toUpperCase()+"' "+
                "and ae.genome_id='"+genomeVer+"' "+
                "and ( "+
                "(ae.psstart >= "+min+" and ae.psstart <="+max+") OR "+
                "(ae.psstop >= "+min+" and ae.psstop <= "+max+") OR "+
                "(ae.psstart <= "+min+" and ae.psstop >="+min+") )"+
                "and ae.psannotation = 'transcript' " +
                "and ae.updatedlocation = 'Y' "+
                "and ae.Array_TYPE_ID = " + arrayTypeID +" )";*/

        
        log.debug("PSLEVEL SQL:"+probeQuery);
        log.debug("Transcript Level SQL:"+probeTransQuery);
            String pListFile=outputDir+"tmp_psList.txt";
            try{
                BufferedWriter psout=new BufferedWriter(new FileWriter(new File(pListFile)));
                Connection conn=null;
                try{
                    conn=pool.getConnection();
                    PreparedStatement ps = conn.prepareStatement(probeQuery);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int psid = rs.getInt(1);
                        psout.write(psid + "\n");
                    }
                    ps.close();
                    conn.close();
                }catch(SQLException ex){
                    log.error("Error getting exon probesets",ex);
                }finally{
                    try {
                        if(conn!=null)
                            conn.close();
                    } catch (SQLException ex) {
                    }
                }
                psout.flush();
                psout.close();
            }catch(IOException e){
                log.error("Error writing exon probesets",e);
            }
            
            ArrayList<GeneLoc> geneList=GeneLoc.readGeneListFile(outputDir,log);
            log.debug("Read in gene list:"+geneList.size());
            String ptransListFiletmp = outputDir + "tmp_psList_transcript.txt";
            //String ptransListFile = outputDir + "tmp_psList_transcript.txt";
            //File srcFile=new File(ptransListFiletmp);
            //File destFile=new File(ptransListFile);
            //try{
                StringBuffer sb=new StringBuffer();
                //BufferedWriter psout = new BufferedWriter(new FileWriter(srcFile));
                Connection conn=null;
                try{
                    conn=pool.getConnection();
                    PreparedStatement ps = conn.prepareStatement(probeTransQuery);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int psid = rs.getInt(1);
                        //log.debug("read ps:"+psid);
                        String ch = rs.getString(2);
                        long start = rs.getLong(3);
                        long stop = rs.getLong(4);
                        String level=rs.getString(5);
                        String strand=rs.getString(6);
                        
                        String ensemblId="",ensGeneSym="";
                        double maxOverlapTC=0.0,maxOverlapGene=0.0,maxComb=0.0;
                        GeneLoc maxGene=null;
                        for(int i=0;i<geneList.size();i++){
                            GeneLoc tmpLoc=geneList.get(i);
                            //log.debug("strand:"+tmpLoc.getStrand()+":"+strand);
                            if(tmpLoc.getStrand().equals(strand)){
                                long maxStart=tmpLoc.getStart();
                                long minStop=tmpLoc.getStop();
                                if(start>maxStart){
                                    maxStart=start;
                                }
                                if(stop<minStop){
                                    minStop=stop;
                                }
                                long genLen=tmpLoc.getStop()-tmpLoc.getStart();
                                long tcLen=stop-start;
                                double overlapLen=minStop-maxStart;
                                double curTCperc=0.0,curGperc=0.0,comb=0.0;
                                if(overlapLen>0){
                                    curTCperc=overlapLen/tcLen*100;
                                    curGperc=overlapLen/tcLen*100;
                                    comb=curTCperc+curGperc;
                                    if(comb>maxComb){
                                        maxOverlapTC=curTCperc;
                                        maxOverlapGene=curGperc;
                                        maxComb=comb;
                                        maxGene=tmpLoc;
                                    }
                                }
                            }
                        }
                        if(maxGene!=null){
                            String tmpGS=maxGene.getGeneSymbol();
                            if(tmpGS.equals("")){
                                tmpGS=maxGene.getID();
                            }
                            //log.debug("out:"+psid + "\t" + ch + "\t" + start + "\t" + stop + "\t" + level + "\t"+tmpGS+"\n");
                            sb.append(psid + "\t" + ch + "\t" + start + "\t" + stop + "\t" + level + "\t"+tmpGS+"\n");
                            
                        }else{
                            //log.debug("out"+psid + "\t" + ch + "\t" + start + "\t" + stop + "\t" + level + "\t\n");
                            sb.append(psid + "\t" + ch + "\t" + start + "\t" + stop + "\t" + level + "\t\n");
                            
                        }
                    }
                    ps.close();
                    conn.close();
                }catch(SQLException ex){
                    log.error("Error getting transcript probesets",ex);
                }finally{
                    try {
                        if(conn!=null)
                            conn.close();
                    } catch (SQLException ex) {
                    }
                }
                try{
                    //log.debug("To File:"+ptransListFiletmp+"\n\n"+sb.toString());
                    myFH.writeFile(sb.toString(),ptransListFiletmp);
                    log.debug("DONE");
                }catch(IOException e){
                    log.error("Error outputing transcript ps list.",e);
                }
                /*psout.flush();
                psout.close();
            }catch(IOException e){
                log.error("Error writing transcript probesets",e);
            }*/
            //srcFile.renameTo(destFile);
            
    }
    
    public boolean createCircosFiles(String perlScriptDirectory, String perlEnvironmentVariables, String[] perlScriptArguments,String filePrefixWithPath){
   		// 
   	    boolean completedSuccessfully=false;
   	    String circosErrorMessage;

   
        //set environment variables so you can access oracle. Environment variables are pulled from perlEnvironmentVariables which is a comma separated list
        String[] envVar=perlEnvironmentVariables.split(",");
    
        for (int i = 0; i < envVar.length; i++) {
            log.debug(i + " EnvVar::" + envVar[i]);
        }
        
       
        //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
        myExec_session = new ExecHandler(perlScriptDirectory, perlScriptArguments, envVar, filePrefixWithPath);
        boolean exception = false;
        try {

            myExec_session.runExec();
            int exit=myExec_session.getExitValue();
            if(exit==0){
                completedSuccessfully=true;
            }else{
                completedSuccessfully=false;
            }
        } catch (ExecException e) {
            exception = true;
            log.error("In Exception of createCircosFiles Exec_session", e);
            Email myAdminEmail = new Email();
            myAdminEmail.setSubject("Exception thrown in Exec_session");
            circosErrorMessage = "There was an error while running ";
            circosErrorMessage = circosErrorMessage + " " + perlScriptArguments[1] + " (";
            for(int i=2; i<perlScriptArguments.length; i++){
            	circosErrorMessage = circosErrorMessage + " " + perlScriptArguments[i];
            }
            circosErrorMessage = circosErrorMessage + ")\n\n"+myExec_session.getErrors();
            if(! circosErrorMessage.contains("WARNING **: Unimplemented style property SP_PROP_POINTER_EVENTS:")){
                myAdminEmail.setContent(circosErrorMessage);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                }
            }
        }
        
        String errors=myExec_session.getErrors();
        if(!exception && errors!=null && !(errors.equals(""))){
            if(! errors.contains("WARNING **: Unimplemented style property SP_PROP_POINTER_EVENTS:")){
                Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in Exec_session");
                circosErrorMessage = "There was an error while running ";
                circosErrorMessage = circosErrorMessage + " " + perlScriptArguments[1] + " (";
                for(int i=2; i<perlScriptArguments.length; i++){
                    circosErrorMessage = circosErrorMessage + " " + perlScriptArguments[i];
                }
                circosErrorMessage = circosErrorMessage + ")\n\n"+errors;
                myAdminEmail.setContent(circosErrorMessage);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                }
            }
        }
   	return completedSuccessfully;
   } 
    

    public boolean createXMLFiles(String organism,String genomeVer,String ensemblIDList,String ensemblID1,String ensemblPath){
        boolean completedSuccessfully=false;
        if(ensemblIDList!=null && ensemblID1!=null && !ensemblIDList.equals("") && !ensemblID1.equals("")){
        try{
            log.debug(ensemblIDList+"\n\n"+ensemblID1);
            //Connection tmpConn=pool.getConnection();
            int publicUserID=new User().getUser_id("public",pool);
            //tmpConn.close();
            log.debug("createXML outputDir:"+outputDir);
            File outDir=new File(outputDir);
            if(outDir.exists()){
                outDir.mkdirs();
            }
            log.debug("after mkdir");
            Properties myProperties = new Properties();
            File myPropertiesFile = new File(dbPropertiesFile);
            myProperties.load(new FileInputStream(myPropertiesFile));

            String dsn="dbi:"+myProperties.getProperty("PLATFORM") +":database="+myProperties.getProperty("DATABASE")+":host="+myProperties.getProperty("HOST");
            String dbUser=myProperties.getProperty("USER");
            String dbPassword=myProperties.getProperty("PASSWORD");
            log.debug("after dbprop");
            File ensPropertiesFile = new File(ensemblDBPropertiesFile);
            Properties myENSProperties = new Properties();
            myENSProperties.load(new FileInputStream(ensPropertiesFile));
            String ensHost=myENSProperties.getProperty("HOST");
            String ensPort=myENSProperties.getProperty("PORT");
            String ensUser=myENSProperties.getProperty("USER");
            String ensPassword=myENSProperties.getProperty("PASSWORD");
            log.debug("after ens dbprop");
            //construct perl Args
            String[] perlArgs = new String[14];
            perlArgs[0] = "perl";
            perlArgs[1] = perlDir + "findGeneRegion.pl";
            perlArgs[2] = outputDir;
            log.debug("perl org:"+organism);
            if (organism.equals("Rn")) {
                perlArgs[3] = "Rat";
            } else if (organism.equals("Mm")) {
                perlArgs[3] = "Mouse";
            }
            perlArgs[4] = "Core";
            perlArgs[5] = ensemblIDList;
            perlArgs[6] = Integer.toString(publicUserID);
            perlArgs[7] = dsn;
            perlArgs[8] = dbUser;
            perlArgs[9] = dbPassword;
            perlArgs[10] = ensHost;
            perlArgs[11] = ensPort;
            perlArgs[12] = ensUser;
            perlArgs[13] = ensPassword;
            
            log.debug("after perl args");
            log.debug("setup params");
            //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
             String[] envVar=perlEnvVar.split(",");

            for (int i = 0; i < envVar.length; i++) {
                if(envVar[i].contains("/ensembl")){
                    envVar[i]=envVar[i].replaceFirst("/ensembl", "/"+ensemblPath);
                }
                log.debug(i + " EnvVar::" + envVar[i]);
            }

            log.debug("setup envVar");
            //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
            myExec_session = new ExecHandler(perlDir, perlArgs, envVar, fullPath + "tmpData/browserCache/"+genomeVer+"/geneData/"+ensemblID1+"/");
            boolean exception=false;
            boolean missingDB=false;
            log.debug("setup exec");
            try {

                myExec_session.runExec();
                log.debug("after exec No Exception");
            } catch (ExecException e) {
                exception = true;
                completedSuccessfully=false;
                log.error("In Exception of run findGeneRegion.pl Exec_session", e);
                
                String errorList=myExec_session.getErrors();
                
                String apiVer="";
                
                    if(errorList.contains("does not exist in DB.")){
                        missingDB=true;
                    }
                    if(errorList.contains("Ensembl API version =")){
                        int apiStart=errorList.indexOf("Ensembl API version =")+22;
                        apiVer=errorList.substring(apiStart,apiStart+3);
                    }
                Email myAdminEmail = new Email();
                if(!missingDB){
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    setError("Running Perl Script to get Gene and Transcript details/images. Ensembl Assembly v"+apiVer);
                }else{
                    myAdminEmail.setSubject("Missing Ensembl ID in DB");
                    setError("The current Ensembl database does not have an entry for this gene ID."+
                                " As Ensembl IDs are added/removed from new versions it is likely this ID has been removed."+
                                " If you used a Gene Symbol and reached this the administrator will investigate. "+
                                "If you entered this Ensembl ID please try to use a synonym or visit Ensembl to investigate the status of this ID. "+
                                "Ensembl Assembly v"+apiVer);
                                        
                }
                
                String errors=myExec_session.getErrors();
                
                myAdminEmail.setContent("There was an error while running "
                        + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+","+perlArgs[7]+
                        ")\n\n"+errors);
                try {
                    if(!missingDB && errors!=null &&errors.length()>0){
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    }
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }

            String errors=myExec_session.getErrors();
            log.debug("after read Exec Errors");
            if(!missingDB && errors!=null && !(errors.equals(""))){
                completedSuccessfully=false;
                Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in Exec_session");
                myAdminEmail.setContent("There was an error while running "
                        + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+
                        ")\n\n"+errors);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }else{
                completedSuccessfully=true;
            }
            log.debug("after if Exec Errors");
            completedSuccessfully=true;
        }catch(Exception e){
            completedSuccessfully=false;
            log.error("Error getting DB properties or Public User ID.",e);
            String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
            Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in GeneDataTools.java");
                myAdminEmail.setContent("There was an error setting up to run findGeneRegion.pl.pl\n\nFull Stacktrace:\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
        }
        }
        return completedSuccessfully;
    }
    public String generateXMLTrack(String chromosome,int min,int max,String panel,String track,String organism,String genomeVer,int rnaDatasetID,int arrayTypeID,String folderName,int binSize){
        String status="";
        try{
            //Connection tmpConn=pool.getConnection();
            log.debug("before get public user id");
            int publicUserID=(new User()).getUser_id("public",pool);
            log.debug("PUBLIC USER ID:"+publicUserID);
            //tmpConn.close();
            String tmpOutputDir=fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/"+folderName+"/";
            
            HashMap<String,String> source=this.getGenomeVersionSource(genomeVer);
            String ensemblPath=source.get("ensembl");
            Properties myProperties = new Properties();
            File myPropertiesFile = new File(dbPropertiesFile);
            myProperties.load(new FileInputStream(myPropertiesFile));

            String dsn="dbi:"+myProperties.getProperty("PLATFORM") +":database="+myProperties.getProperty("DATABASE")+":host="+myProperties.getProperty("HOST");
            String dbUser=myProperties.getProperty("USER");
            String dbPassword=myProperties.getProperty("PASSWORD");
            
            Properties myVerProperties = new Properties();
            log.debug("UCSC file:"+ucscDBVerPropertiesFile);
            File myVerPropertiesFile = new File(ucscDBVerPropertiesFile);
            myVerProperties.load(new FileInputStream(myVerPropertiesFile));
            log.debug("read prop");
            //String dbVer=myVerProperties.getProperty("UCSCDATE");
            //String refSeqDB=organism+"_"+myVerProperties.getProperty("REFSEQVER");
            String ucscHost=myVerProperties.getProperty("HOST");
            String ucscPort=myVerProperties.getProperty("PORT");
            String ucscUser=myVerProperties.getProperty("USER");
            String ucscPassword=myVerProperties.getProperty("PASSWORD");
            
            File ensPropertiesFile = new File(ensemblDBPropertiesFile);
            Properties myENSProperties = new Properties();
            myENSProperties.load(new FileInputStream(ensPropertiesFile));
            String ensHost=myENSProperties.getProperty("HOST");
            String ensPort=myENSProperties.getProperty("PORT");
            String ensUser=myENSProperties.getProperty("USER");
            String ensPassword=myENSProperties.getProperty("PASSWORD");
            
            File mongoPropertiesFile = new File(mongoDBPropertiesFile);
            Properties myMongoProperties = new Properties();
            myMongoProperties.load(new FileInputStream(mongoPropertiesFile));
            String mongoHost=myMongoProperties.getProperty("HOST");
            String mongoUser=myMongoProperties.getProperty("USER");
            String mongoPassword=myMongoProperties.getProperty("PASSWORD");
            
            /*String refSeqDB="Rn_refseq_5";
            if(organism.equals("Mm")){
                refSeqDB="Mm_refseq_5";
            }*/
            /*String genome="rn5";
            if(organism.equals("Mm")){
                genome="mm10";
            }*/
            log.debug("done properties");
            
            //NEED TO MODIFY*************************
            String ensDsn="DBI:mysql:database="+source.get("ucsc")+";host="+ucscHost+";port="+ucscPort+";";
            String ucscDsn="DBI:mysql:database="+source.get("ucsc")+";host="+ucscHost+";port="+ucscPort+";";
            //NEED TO MODIFY******************************************************************************************************
            String tissue="Brain";
            if(track.startsWith("liver")){
                tissue="Liver";
            }else if(track.startsWith("heart")){
                tissue="Heart";
            }else if(track.startsWith("merged")){
                tissue="Merged";
            }
            
            //construct perl Args
            String[] perlArgs = new String[25];
            perlArgs[0] = "perl";
            perlArgs[1] = perlDir + "writeXML_Track.pl";
            perlArgs[2] = tmpOutputDir;
            if (organism.equals("Rn")) {
                perlArgs[3] = "Rat";
            }else if (organism.equals("Mm")) {
                perlArgs[3] = "Mouse";
            }
            perlArgs[4] = track;
            perlArgs[5] = panel;
            perlArgs[6]=chromosome;
             perlArgs[7] = Integer.toString(min);
            perlArgs[8] = Integer.toString(max);
            perlArgs[9] = Integer.toString(publicUserID);
            perlArgs[10] = Integer.toString(binSize);
            perlArgs[11] = tissue;
            perlArgs[12] = genomeVer;
            perlArgs[13] = dsn;
            perlArgs[14] = dbUser;
            perlArgs[15] = dbPassword;
            perlArgs[16] = ensDsn;
            perlArgs[17] = ensUser;
            perlArgs[18] = ensPassword;
            perlArgs[19] = ucscDsn;
            perlArgs[20] = ucscUser;
            perlArgs[21] = ucscPassword;
            perlArgs[22] = mongoHost;
            perlArgs[23] = mongoUser;
            perlArgs[24] = mongoPassword;

            //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
            String[] envVar=perlEnvVar.split(",");

            for (int i = 0; i < envVar.length; i++) {
                if(envVar[i].contains("/ensembl")){
                    envVar[i]=envVar[i].replaceAll("/ensembl", "/"+ensemblPath);
                }
                log.debug(i + " EnvVar::" + envVar[i]);
            }
            //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
            myExec_session = new ExecHandler(perlDir, perlArgs, envVar, fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/"+folderName+"/");
            boolean exception=false;
            try {
                myExec_session.runExec();
                status="successful";
            } catch (ExecException e) {
                exception=true;
                e.printStackTrace(System.err);
                status="Error generating track";
                log.error("In Exception of run writeXML_Track.pl Exec_session", e);
                String errors=myExec_session.getErrors();
                if(errors!=null && errors.length()>0 ){
                    setError("Running Perl Script to get Gene and Transcript details/images.");
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+","+perlArgs[7]+","+perlArgs[8]+
                            ")\n\n"+errors);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }
            }

            String errors=myExec_session.getErrors();
            //log.debug("Error String:"+errors);
            if(!exception && errors!=null && !(errors.equals(""))){
                status="Error generating track";
                Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Error is not null in Exec_session");
                myAdminEmail.setContent("There was not an exception but error output was not empty while running "
                        + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+
                        ")\n\n"+errors);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }
        }catch(Exception e){
            status="Error generating track";
            log.error("Error getting DB properties or Public User ID.",e);
            String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
            Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in GeneDataTools.java");
                myAdminEmail.setContent("There was an error setting up to run writeXML_Track.pl\n\nFull Stacktrace:\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
        }
        return status;
    }
    
     public String generateCustomBedXMLTrack(String chromosome,int min,int max,String track,String organism,String folder,String bedFile,String outputFile){
        String status="";
        try{        
            //construct perl Args
            String[] perlArgs = new String[7];
            perlArgs[0] = "perl";
            perlArgs[1] = perlDir + "bed2XML.pl";
            perlArgs[2] = fullPath +bedFile;
            perlArgs[3] = fullPath +outputFile;
            
            perlArgs[4] = Integer.toString(min);
            perlArgs[5] = Integer.toString(max);
            perlArgs[6] = chromosome;

            File dir=new File(fullPath + "tmpData/trackXML/"+folder+"/");
            if(dir.exists()||dir.mkdirs()){
                for (int i = 0; i < perlArgs.length; i++) {
                    log.debug(i + " perlArgs::" + perlArgs[i]);
                }
                //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
                String[] envVar=perlEnvVar.split(",");
                
                for (int i = 0; i < envVar.length; i++) {
                    log.debug(i + " EnvVar::" + envVar[i]);
                    /*if(envVar[i].startsWith("PERL5LIB")&&organism.equals("Mm")){
                        envVar[i]=envVar[i].replaceAll("ensembl_ucsc", "ensembl_ucsc_old");
                    }*/
                }
                //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
                myExec_session = new ExecHandler(perlDir, perlArgs, envVar, fullPath + "tmpData/trackXML/"+folder+"/");
                boolean exception=false;
                try {
                    myExec_session.runExec();
                    status="successful";
                } catch (ExecException e) {
                    exception=true;
                    status="Error generating track";
                    log.error("In Exception of run bed2XML.pl Exec_session", e);
                    setError("Running Perl Script to get Gene and Transcript details/images.");
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+","+perlArgs[7]+","+perlArgs[8]+
                            ")\n\n"+myExec_session.getErrors());
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }

                String errors=myExec_session.getErrors();
                if(!exception && errors!=null && !(errors.equals(""))){
                    status="Error generating track";
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+
                            ")\n\n"+errors);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }else{

                }
            }
        }catch(Exception e){
            status="Error generating track";
            log.error("Error getting DB properties or Public User ID.",e);
            String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
            Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in GeneDataTools.java");
                myAdminEmail.setContent("There was an error setting up to run bed2XML.pl\n\nFull Stacktrace:\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
        }
        return status;
    }
    
    public String generateCustomBedGraphXMLTrack(String chromosome,int min,int max,String track,String organism,String folder,String bedFile,String outputFile,int binSize){
        String status="";
        try{        
            //construct perl Args
            String[] perlArgs = new String[8];
            perlArgs[0] = "perl";
            perlArgs[1] = perlDir + "bedGraph2XML.pl";
            perlArgs[2] = fullPath+bedFile;
            perlArgs[3] = fullPath+outputFile;
            perlArgs[4] = Integer.toString(min);
            perlArgs[5] = Integer.toString(max);
            perlArgs[6] = chromosome;
            perlArgs[7] = Integer.toString(binSize);
            File dir=new File(fullPath + "tmpData/trackXML/"+folder+"/");
            if(dir.exists()||dir.mkdirs()){
                for (int i = 0; i < perlArgs.length; i++) {
                    log.debug(i + " perlArgs::" + perlArgs[i]);
                }
                //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
                String[] envVar=perlEnvVar.split(",");
                
                //for (int i = 0; i < envVar.length; i++) {
                //    log.debug(i + " EnvVar::" + envVar[i]);
                    /*if(envVar[i].startsWith("PERL5LIB")&&organism.equals("Mm")){
                        envVar[i]=envVar[i].replaceAll("ensembl_ucsc", "ensembl_ucsc_old");
                    }*/
                //}
                //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
                myExec_session = new ExecHandler(perlDir, perlArgs, envVar, fullPath + "tmpData/trackXML/"+folder+"/");
                boolean exception=false;
                try {
                    myExec_session.runExec();
                    status="successful";
                } catch (ExecException e) {
                    exception=true;
                    status="Error generating track";
                    log.error("In Exception of run bed2XML.pl Exec_session", e);
                    setError("Running Perl Script to get Gene and Transcript details/images.");
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+","+perlArgs[7]+","+perlArgs[8]+
                            ")\n\n"+myExec_session.getErrors());
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }

                String errors=myExec_session.getErrors();
                if(!exception && errors!=null && !(errors.equals(""))){
                    status="Error generating track";
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+
                            ")\n\n"+errors);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }else{

                }
            }
        }catch(Exception e){
            status="Error generating track";
            log.error("Error getting DB properties or Public User ID.",e);
            String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
            Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in GeneDataTools.java");
                myAdminEmail.setContent("There was an error setting up to run bed2XML.pl\n\nFull Stacktrace:\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
        }
        return status;
    }
    
    public String generateCustomRemoteXMLTrack(String chromosome,int min,int max,String track,String organism,String folder,String bedFile,String outputFile,String type,String url,int binSize){
        String status="";
        int paramSize=7;
        String function="bigBed2XML.pl";
        //String fullBed=fullPath+bedFile;
        if(type.equals("bw")){
                paramSize=8;
                function="bigWig2XML.pl";
                //fullBed=url;
        }
        String[] perlArgs = new String[paramSize];
        perlArgs[0] = "perl";
        perlArgs[1] = perlDir + function;
        perlArgs[2] = url;
        perlArgs[3] = fullPath+outputFile;
        perlArgs[4] = Integer.toString(min);
        perlArgs[5] = Integer.toString(max);
        perlArgs[6] = chromosome;
        if(type.equals("bw")){
            perlArgs[7]=Integer.toString(binSize);
        }
        try{        
            //construct perl Args
            

            File dir=new File(fullPath + "tmpData/trackXML/"+folder+"/");
            if(dir.exists()||dir.mkdirs()){
                for (int i = 0; i < perlArgs.length; i++) {
                    log.debug(i + " perlArgs::" + perlArgs[i]);
                }
                //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
                String[] envVar=perlEnvVar.split(",");
                
                //for (int i = 0; i < envVar.length; i++) {
                //    log.debug(i + " EnvVar::" + envVar[i]);
                    /*if(envVar[i].startsWith("PERL5LIB")&&organism.equals("Mm")){
                        envVar[i]=envVar[i].replaceAll("ensembl_ucsc", "ensembl_ucsc_old");
                    }*/
                //}
                //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
                myExec_session = new ExecHandler(perlDir, perlArgs, envVar, fullPath + "tmpData/trackXML/"+folder+"/");
                boolean exception=false;
                try {
                    myExec_session.runExec();
                    status="successful";
                } catch (ExecException e) {
                    exception=true;
                    status="Error generating track";
                    log.error("In Exception of run bed2XML.pl Exec_session", e);
                    setError("Running Perl Script to get Gene and Transcript details/images.");
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+","+perlArgs[7]+","+perlArgs[8]+
                            ")\n\n"+myExec_session.getErrors());
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }

                String errors=myExec_session.getErrors();
                if(!exception && errors!=null && !(errors.equals(""))){
                    status="Error generating track";
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+
                            ")\n\n"+errors);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }else{

                }
            }
        }catch(Exception e){
            status="Error generating track";
            log.error("Error getting DB properties or Public User ID.",e);
            String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
            Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in GeneDataTools.java");
                myAdminEmail.setContent("There was an error setting up to run "+function+"\n\nFull Stacktrace:\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
        }
        return status;
    }
     
    /*public boolean createRegionImagesXMLFiles(String folderName,String organism,String genomeVer,String ensemblPath,int arrayTypeID,int rnaDatasetID,String ucscDB){
        boolean completedSuccessfully=false;
        try{
            //Connection tmpConn=pool.getConnection();
            int publicUserID=new User().getUser_id("public",pool);
            //tmpConn.close();
            Properties myProperties = new Properties();
            File myPropertiesFile = new File(dbPropertiesFile);
            myProperties.load(new FileInputStream(myPropertiesFile));

            String dsn="dbi:"+myProperties.getProperty("PLATFORM") +":database="+myProperties.getProperty("DATABASE")+":host="+myProperties.getProperty("HOST");
            String dbUser=myProperties.getProperty("USER");
            String dbPassword=myProperties.getProperty("PASSWORD");

            File ensPropertiesFile = new File(ensemblDBPropertiesFile);
            Properties myENSProperties = new Properties();
            myENSProperties.load(new FileInputStream(ensPropertiesFile));
            String ensHost=myENSProperties.getProperty("HOST");
            String ensPort=myENSProperties.getProperty("PORT");
            String ensUser=myENSProperties.getProperty("USER");
            String ensPassword=myENSProperties.getProperty("PASSWORD");
            
            File mongoPropertiesFile = new File(mongoDBPropertiesFile);
            Properties myMongoProperties = new Properties();
            myMongoProperties.load(new FileInputStream(mongoPropertiesFile));
            String mongoHost=myMongoProperties.getProperty("HOST");
            String mongoUser=myMongoProperties.getProperty("USER");
            String mongoPassword=myMongoProperties.getProperty("PASSWORD");
            
            //construct perl Args
            String[] perlArgs = new String[25];
            perlArgs[0] = "perl";
            perlArgs[1] = perlDir + "writeXML_Region.pl";
            perlArgs[2] = ucscDir+ucscGeneDir;
            perlArgs[3] = outputDir;
            perlArgs[4] = folderName;
            if (organism.equals("Rn")) {
                perlArgs[5] = "Rat";
            } else if (organism.equals("Mm")) {
                perlArgs[5] = "Mouse";
            }
            perlArgs[6] = "Core";
            if(chrom.startsWith("chr")){
                chrom=chrom.substring(3);
            }
            perlArgs[7] = chrom;
            perlArgs[8] = Integer.toString(minCoord);
            perlArgs[9] = Integer.toString(maxCoord);
            perlArgs[10] = Integer.toString(arrayTypeID);
            perlArgs[11] = Integer.toString(rnaDatasetID);
            perlArgs[12] = Integer.toString(publicUserID);
            perlArgs[13] = genomeVer;
            perlArgs[14] = dsn;
            perlArgs[15] = dbUser;
            perlArgs[16] = dbPassword;
            perlArgs[17] = ucscDB;
            perlArgs[18] = ensHost;
            perlArgs[19] = ensPort;
            perlArgs[20] = ensUser;
            perlArgs[21] = ensPassword;
            perlArgs[22] = mongoHost;
            perlArgs[23] = mongoUser;
            perlArgs[24] = mongoPassword;


            //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
            String[] envVar=perlEnvVar.split(",");

            for (int i = 0; i < envVar.length; i++) {
                if(envVar[i].contains("/ensembl")){
                    envVar[i]=envVar[i].replaceAll("/ensembl","/"+ensemblPath);
                }
                log.debug(i + " EnvVar::" + envVar[i]);
            }


            //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
            myExec_session = new ExecHandler(perlDir, perlArgs, envVar, outputDir+"genRegion");
            boolean exception=false;
            try {

                myExec_session.runExec();

            } catch (ExecException e) {
                exception=true;
                log.error("In Exception of run writeXML_Region.pl Exec_session", e);
                setError("Running Perl Script to get Gene and Transcript details/images.");
                Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in Exec_session");
                myAdminEmail.setContent("There was an error while running "
                        + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+","+perlArgs[7]+","+perlArgs[8]+","+perlArgs[9]+","+perlArgs[10]+","+perlArgs[11]+
                        ")\n\n"+myExec_session.getErrors());
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }

            String errors=myExec_session.getErrors();
            log.debug("ERRORS:\n:"+errors+":");
            if(!exception && errors!=null && !(errors.equals(""))){
                Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in Exec_session");
                myAdminEmail.setContent("There was an error while running "
                        + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+
                        ")\n\n"+errors);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
            }else{
                completedSuccessfully=true;
            }
        }catch(Exception e){
            log.error("Error getting DB properties or Public User ID.",e);
            String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
            Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in GeneDataTools.java");
                myAdminEmail.setContent("There was an error setting up to run writeXML_Region.pl\n\nFull Stacktrace:\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
        }
        return completedSuccessfully;
    }*/
    
    public AsyncGeneDataTools callAsyncGeneDataTools(String chr, int min, int max,int arrayTypeID,int rnaDS_ID,String genomeVer,boolean isENSGene){
        AsyncGeneDataTools agdt;         
        agdt = new AsyncGeneDataTools(session,pool,outputDir,chr, min, max,arrayTypeID,rnaDS_ID,usageID,genomeVer,isENSGene);
        //log.debug("Getting ready to start");
        agdt.start();
        //log.debug("Started AsyncGeneDataTools");
        return agdt;
    }
    
    
    
    public boolean callWriteXML(String id,String organism,String genomeVer,String chr, int min, int max,int arrayTypeID,int rnaDS_ID){
        boolean completedSuccessfully=false;
        log.debug("callWriteXML()"+id+","+organism+","+genomeVer+","+arrayTypeID+","+rnaDS_ID);
        try{
            //Connection tmpConn=pool.getConnection();
            int publicUserID=new User().getUser_id("public",pool);
            //tmpConn.close();
            String tmpoutputDir = fullPath + "tmpData/browserCache/"+genomeVer+"/geneData/" + id + "/";
            HashMap<String,String> source=this.getGenomeVersionSource(genomeVer);
            String ensemblPath=source.get("ensembl");
            File test=new File(tmpoutputDir+"Region.xml");
            long testLM=test.lastModified();
            testLM=(new Date().getTime())-testLM;
            long fifteenMin=15*60*1000;
            if(!test.exists() || (test.length()==0 && testLM>fifteenMin)){
                log.debug("createXML outputDir:"+tmpoutputDir);
                File outDir=new File(tmpoutputDir);
                if(outDir.exists()){
                    outDir.mkdirs();
                }
                Properties myProperties = new Properties();
                File myPropertiesFile = new File(dbPropertiesFile);
                myProperties.load(new FileInputStream(myPropertiesFile));

                String dsn="dbi:"+myProperties.getProperty("PLATFORM") +":database="+myProperties.getProperty("DATABASE")+":host="+myProperties.getProperty("HOST");
                String dbUser=myProperties.getProperty("USER");
                String dbPassword=myProperties.getProperty("PASSWORD");

                File ensPropertiesFile = new File(ensemblDBPropertiesFile);
                Properties myENSProperties = new Properties();
                myENSProperties.load(new FileInputStream(ensPropertiesFile));
                String ensHost=myENSProperties.getProperty("HOST");
                String ensPort=myENSProperties.getProperty("PORT");
                String ensUser=myENSProperties.getProperty("USER");
                String ensPassword=myENSProperties.getProperty("PASSWORD");

                File mongoPropertiesFile = new File(mongoDBPropertiesFile);
                Properties myMongoProperties = new Properties();
                myMongoProperties.load(new FileInputStream(mongoPropertiesFile));
                String mongoHost=myMongoProperties.getProperty("HOST");
                String mongoUser=myMongoProperties.getProperty("USER");
                String mongoPassword=myMongoProperties.getProperty("PASSWORD");
                log.debug("loaded properties");

                //construct perl Args
                String[] perlArgs = new String[21];
                perlArgs[0] = "perl";
                perlArgs[1] = perlDir + "writeXML_RNA.pl";
                perlArgs[2] = tmpoutputDir;
                if (organism.equals("Rn")) {
                    perlArgs[3] = "Rat";
                } else if (organism.equals("Mm")) {
                    perlArgs[3] = "Mouse";
                }
                perlArgs[4] = "Core";
                perlArgs[5] = id;
                perlArgs[6] = ucscDir+ucscGeneDir;
                perlArgs[7] = Integer.toString(arrayTypeID);
                perlArgs[8] = Integer.toString(rnaDS_ID);
                perlArgs[9] = Integer.toString(publicUserID);
                perlArgs[10]= genomeVer;
                perlArgs[11] = dsn;
                perlArgs[12] = dbUser;
                perlArgs[13] = dbPassword;
                perlArgs[14] = ensHost;
                perlArgs[15] = ensPort;
                perlArgs[16] = ensUser;
                perlArgs[17] = ensPassword;
                perlArgs[18] = mongoHost;
                perlArgs[19] = mongoUser;
                perlArgs[20] = mongoPassword;


                log.debug("setup params");
                //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
                String[] envVar=perlEnvVar.split(",");

                for (int i = 0; i < envVar.length; i++) {
                    if(envVar[i].contains("/ensembl")){
                        envVar[i]=envVar[i].replaceFirst("/ensembl", "/"+ensemblPath);
                    }
                    log.debug(i + " EnvVar::" + envVar[i]);
                }
                log.debug("setup envVar");
                //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
                myExec_session = new ExecHandler(perlDir, perlArgs, envVar, fullPath + "tmpData/browserCache/"+genomeVer+"/geneData/"+id+"/");
                boolean exception = false;
                try {
                    myExec_session.runExec();
                } catch (ExecException e) {
                    exception=true;
                    completedSuccessfully=false;
                    e.printStackTrace(System.err);
                    log.error("In Exception of run callWriteXML:writeXML_RNA.pl Exec_session", e);

                    String errorList=myExec_session.getErrors();
                    boolean missingDB=false;
                    String apiVer="";

                        if(errorList.contains("does not exist in DB.")){
                            missingDB=true;
                        }
                        if(errorList.contains("Ensembl API version =")){
                            int apiStart=errorList.indexOf("Ensembl API version =")+22;
                            apiVer=errorList.substring(apiStart,apiStart+3);
                        }
                    Email myAdminEmail = new Email();
                    if(!missingDB){
                        myAdminEmail.setSubject("Exception thrown in Exec_session");
                        setError("Running Perl Script to get Gene and Transcript details/images. Ensembl Assembly v"+apiVer);
                    }else{
                        myAdminEmail.setSubject("Missing Ensembl ID in DB");
                        setError("The current Ensembl database does not have an entry for this gene ID."+
                                    " As Ensembl IDs are added/removed from new versions it is likely this ID has been removed."+
                                    " If you used a Gene Symbol and reached this the administrator will investigate. "+
                                    "If you entered this Ensembl ID please try to use a synonym or visit Ensembl to investigate the status of this ID. "+
                                    "Ensembl Assembly v"+apiVer);

                    }

                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+","+perlArgs[7]+
                            ")\n\n"+myExec_session.getErrors());
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }

                String errors=myExec_session.getErrors();
                if(!exception && errors!=null && !(errors.equals(""))){
                    completedSuccessfully=false;
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+" , "+perlArgs[5]+" , "+perlArgs[6]+
                            ")\n\n"+errors);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }
            }
        }catch(Exception e){
            completedSuccessfully=false;
            log.error("Error getting DB properties or Public User ID.",e);
            String fullerrmsg=e.getMessage();
                    StackTraceElement[] tmpEx=e.getStackTrace();
                    for(int i=0;i<tmpEx.length;i++){
                        fullerrmsg=fullerrmsg+"\n"+tmpEx[i];
                    }
            Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in GeneDataTools.java");
                myAdminEmail.setContent("There was an error setting up to run writeXML_RNA.pl\n\nFull Stacktrace:\n"+fullerrmsg);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }
        }
        return completedSuccessfully;
    }
    
    public boolean callPanelExpr(String id,String chr, int min, int max,String genomeVer,int arrayTypeID,int rnaDS_ID,AsyncGeneDataTools prevThread){
        boolean error=false;
        String organism="Rn";
        if(arrayTypeID==21){
            organism="Mm";
        }
        callWriteXML(id,organism,genomeVer,chr,min,max,arrayTypeID,rnaDS_ID);
        //create File with Probeset Tissue herit and DABG
        /*String datasetQuery="select rd.dataset_id, rd.tissue "+
                            "from rnadataset_dataset rd "+
                            "where rd.rna_dataset_id = "+rnaDS_ID+" "+
                            "order by rd.tissue";
        
        Date start=new Date();
        Connection conn=null;
        try{
            conn=pool.getConnection();
            PreparedStatement ps = conn.prepareStatement(datasetQuery);
            ResultSet rs = ps.executeQuery();
            try{
                String ver="v9";
                if(arrayTypeID==21){
                    ver="v6";
                }
                String tmpOutput= fullPath + "tmpData/browserCache/"+genomeVer+"/geneData/" + id+ "/";
                //log.debug("Getting ready to start");
                File indivf=new File(tmpOutput+"Panel_Expr_indiv.txt");
                File groupf=new File(tmpOutput+"Panel_Expr_group.txt");
                long curTime=new Date().getTime();
                long indLM=indivf.lastModified();
                long groupLM=groupf.lastModified();
                indLM=curTime-indLM;
                groupLM=curTime-groupLM;
                long twoHours=1000*60*10;
                if(!indivf.exists() || !groupf.exists()||((groupf.length()==0 && groupLM>twoHours) || (indivf.length()==0 && indLM>twoHours))){
                    log.debug("\n\ntrying to run\n\n");
                    BufferedWriter outGroup=new BufferedWriter(new FileWriter(groupf));
                    BufferedWriter outIndiv=new BufferedWriter(new FileWriter(indivf));
                    ArrayList<AsyncGeneDataExpr> localList=new ArrayList<AsyncGeneDataExpr>();
                    SyncAndClose sac=new SyncAndClose(start,localList,null,pool,outGroup,outIndiv,usageID,tmpOutput);
                    log.debug("\n\nafter setup\n\n");
                    while(rs.next()){
                        AsyncGeneDataExpr agde=new AsyncGeneDataExpr(session,tmpOutput+"tmp_psList.txt",tmpOutput,null,threadList,maxThreadRunning,outGroup,outIndiv,sac,ver);
                        String dataset_id=Integer.toString(rs.getInt("DATASET_ID"));
                        int iDSID=rs.getInt("DATASET_ID");
                        String tissue=rs.getString("TISSUE");
                        log.debug("\nAGDE for "+iDSID+":"+tissue+"\n");
                        String tissueNoSpaces=tissue.replaceAll(" ", "_");
                        edu.ucdenver.ccp.PhenoGen.data.Dataset sDataSet=new edu.ucdenver.ccp.PhenoGen.data.Dataset();
                        //Connection tmpConn=pool.getConnection();
                        edu.ucdenver.ccp.PhenoGen.data.Dataset curDS=sDataSet.getDataset(iDSID,pool,"");
                        //tmpConn.close();
                        String affyFile="allPS";
                        String verStr="allPS";
                        if(arrayTypeID==21){
                            affyFile="NormVer";
                            verStr=ver;
                        }
                        log.debug("After Dataset before paths");
                        String DSPath=userFilesRoot+"public/Datasets/"+curDS.getNameNoSpaces()+"_Master/Affy."+affyFile+".h5";
                        String sampleFile=userFilesRoot+"public/Datasets/"+curDS.getNameNoSpaces()+"_Master/"+verStr+"_samples.txt";
                        String groupFile=userFilesRoot+"public/Datasets/"+curDS.getNameNoSpaces()+"_Master/"+verStr+"_groups.txt";
                        String outGroupFile="group_"+tissueNoSpaces+"_exprVal.txt";
                        String outIndivFile="indiv_"+tissueNoSpaces+"_exprVal.txt";
                        log.debug("after paths");
                        agde.add(DSPath,sampleFile,groupFile,outGroupFile,outIndivFile,tissue,curDS.getPlatform());
                        log.debug("after add agde");
                        threadList.add(agde);
                        localList.add(agde);
                        log.debug("before start");
                        agde.start();     
                        log.debug("after start");
                    }
                    
                }
                try{
                    ps.close();
                }catch(Exception e){}
                try{
                    conn.close();
                }catch(Exception e){}
                //log.debug("Started AsyncGeneDataExpr");
            }catch(IOException ioe){
                log.error("IOException:\n",ioe);
            }
            
        }catch(SQLException e){
            error=true;
            log.error("Error getting dataset id",e);
            setError("SQL Error occurred while setting up Panel Expression");
        }finally{
           try {
                    if(conn!=null)
                        conn.close();
                } catch (SQLException ex) {
                }
        }*/
        return error;
    }
    
    
    private String getUCSCUrlwoGlobal(String urlFile){
        String ret="error";
        try{
                String[] urls=myFH.getFileContents(new File(urlFile));
                ret=urls[1];
                ret=ret.replaceFirst("&position=", "&pix='800'&position=");
        }catch(IOException e){
                log.error("Error reading url file "+urlFile,e);
        }
        return ret;
    }
    private boolean getUCSCUrl(String urlFile){
        boolean error=false;
        String[] urls;
        try{
                urls=myFH.getFileContents(new File(urlFile));
                this.geneSymbol=urls[0];
                this.returnGeneSymbol=this.geneSymbol;
                if(urls.length>2){
                    this.returnGeneSymbol=urls[2];
                }
                
                //session.setAttribute("geneSymbol", this.geneSymbol);
                this.ucscURL=urls[1];
                this.ucscURL=this.ucscURL.replaceFirst("&position=", "&pix='800'&position=");
                int start=urls[1].indexOf("position=")+9;
                int end=urls[1].indexOf("&",start);
                String position=urls[1].substring(start,end);
                String[] split=position.split(":");
                String chromosome=split[0].substring(3);
                String[] split2=split[1].split("-");
                this.minCoord=Integer.parseInt(split2[0]);
                this.maxCoord=Integer.parseInt(split2[1]);
                this.chrom=chromosome;
                //log.debug(ucscURL+"\n");
        }catch(IOException e){
                log.error("Error reading url file "+urlFile,e);
                setError("Reading URL File");
                error=true;
        }
        return error;
    }
    
    private boolean getUCSCUrls(String ensemblID1){
        boolean error=false;
        String[] urls;
        try{
                urls=myFH.getFileContents(new File(outputDir + ensemblID1+".url"));
                this.geneSymbol=urls[0];
                this.returnGeneSymbol=this.geneSymbol;
                
                //session.setAttribute("geneSymbol", this.geneSymbol);
                this.ucscURL=urls[1];
                int start=urls[1].indexOf("position=")+9;
                int end=urls[1].indexOf("&",start);
                String position=urls[1].substring(start,end);
                String[] split=position.split(":");
                String chromosome=split[0].substring(3);
                String[] split2=split[1].split("-");
                this.minCoord=Integer.parseInt(split2[0]);
                this.maxCoord=Integer.parseInt(split2[1]);
                this.chrom=chromosome;
                //log.debug(ucscURL+"\n");
        }catch(IOException e){
                log.error("Error reading url file "+outputDir + ensemblID1,e);
                setError("Reading URL File");
                error=true;
        }
        return error;
    }

    public String getChromosome() {
        return chrom;
    }

    public int getMinCoord() {
        return minCoord;
    }

    public int getMaxCoord() {
        return maxCoord;
    }
    
    private String loadErrorMessage(){
        String ret="";
        try{
                File err=new File(outputDir +"errMsg.txt");
                if(err.exists()){
                    String[] tmp=myFH.getFileContents(new File(outputDir +"errMsg.txt"));
                    if(tmp!=null){
                        if(tmp.length>=1){
                            ret=tmp[0];
                        }
                        for(int i=1;i<tmp.length;i++){
                            ret=ret+"\n"+tmp;
                        }
                    }
                }
        }catch(IOException e){
                log.error("Error reading errMsg.txt file "+outputDir ,e);
                setError("Reading errMsg File");
        }
        return ret;
    }
    
    private void setError(String errorMessage){
        String tmp=returnGenURL;
        if(tmp==null||tmp.equals("")||!tmp.startsWith("ERROR:")){
            //session.setAttribute("genURL","ERROR: "+errorMessage);
            returnGenURL="ERROR: "+errorMessage;
        }else{
            returnGenURL=returnGenURL+", "+errorMessage;
        }
    }
    
    /*private void setReturnSessionVar(boolean error,String folderName){
        if(!error){
            session.setAttribute("genURL",urlPrefix + "tmpData/geneData/" + folderName + "/");
            session.setAttribute("ucscURL", this.ucscURL);
            session.setAttribute("ucscURLFiltered", this.ucscURLfilter);
            session.setAttribute("curOutputDir",outputDir);
        }else{
            String tmp=(String)session.getAttribute("genURL");
            if(tmp.equals("")||!tmp.startsWith("ERROR:")){
                session.setAttribute("genURL","ERROR:Unknown Error");
            }
            session.setAttribute("ucscURL", "");
            session.setAttribute("ucscURLFiltered", "");
            if(folderName!=null && !folderName.equals("")){
                try{
                    new FileHandler().writeFile((String)session.getAttribute("genURL"),outputDir+"errMsg.txt");
                }catch(IOException e){
                    log.error("Error writing errMsg.txt",e);
                }
            }
        }
    }*/
    
    private void setPublicVariables(boolean error,String genomeVer,String folderName){
        if(!error){
            returnGenURL=urlPrefix + "tmpData/browserCache/"+genomeVer+"/geneData/" + folderName + "/";
            returnUCSCURL= this.ucscURL;
            returnOutputDir=outputDir;
        }else{
            String tmp=returnGenURL;
            if(tmp.equals("")||!tmp.startsWith("ERROR:")){
                returnGenURL="ERROR:Unknown Error";
            }
            returnUCSCURL= "";
            if(folderName!=null && !folderName.equals("")){
                try{
                    new FileHandler().writeFile(returnGenURL,outputDir+"errMsg.txt");
                }catch(IOException e){
                    log.error("Error writing errMsg.txt",e);
                }
            }
        }
        
        
    }
    
    public HttpSession getSession() {
        return session;
    }

    public String formatDate(GregorianCalendar gc) {
        String ret;
        String year = Integer.toString(gc.get(GregorianCalendar.YEAR));
        String month = Integer.toString(gc.get(GregorianCalendar.MONTH) + 1);
        if (month.length() == 1) {
            month = "0" + month;
        }
        String day = Integer.toString(gc.get(GregorianCalendar.DAY_OF_MONTH));
        if (day.length() == 1) {
            day = "0" + day;
        }
        String hour = Integer.toString(gc.get(GregorianCalendar.HOUR_OF_DAY));
        if (hour.length() == 1) {
            hour = "0" + hour;
        }
        String minute = Integer.toString(gc.get(GregorianCalendar.MINUTE));
        if (minute.length() == 1) {
            minute = "0" + minute;
        }
        ret = year + month + day + hour + minute;
        return ret;
    }
    
    public void setSession(HttpSession inSession) {
        //log.debug("in GeneDataTools.setSession");
        this.session = inSession;
        
        //log.debug("start");
        //this.dbConn = (Connection) session.getAttribute("dbConn");
        this.pool= (DataSource) session.getAttribute("dbPool");
        //log.debug("db");
        this.perlDir = (String) session.getAttribute("perlDir") + "scripts/";
        //log.debug("perl"+perlDir);
        String contextRoot = (String) session.getAttribute("contextRoot");
        //log.debug("context"+contextRoot);
        String host = (String) session.getAttribute("host");
        //log.debug("host"+host);
        String appRoot = (String) session.getAttribute("applicationRoot");
        //log.debug("app"+appRoot);
        this.fullPath = appRoot + contextRoot;
        //log.debug("fullpath");
        this.rFunctDir = (String) session.getAttribute("rFunctionDir");
        //log.debug("rFunction");
        
        //this.urlPrefix=(String)session.getAttribute("mainURL");
        //if(urlPrefix.endsWith(".jsp")){
            urlPrefix="https://" + host + contextRoot;
        //}
        //log.debug("mainURL");
        this.perlEnvVar=(String)session.getAttribute("perlEnvVar");
        //log.debug("PerlEnv");
        this.ucscDir=(String)session.getAttribute("ucscDir");
        this.ucscGeneDir=(String)session.getAttribute("ucscGeneDir");
        //log.debug("ucsc");
        this.bedDir=(String) session.getAttribute("bedDir");
        //log.debug("bedDir");
        
        this.dbPropertiesFile = (String)session.getAttribute("dbPropertiesFile");
        this.ensemblDBPropertiesFile = (String)session.getAttribute("ensDbPropertiesFile");
        this.ucscDBVerPropertiesFile = (String)session.getAttribute("ucscDbPropertiesFile");
        this.mongoDBPropertiesFile = (String)session.getAttribute("mongoDbPropertiesFile");
        log.debug("UCSC File:"+ucscDBVerPropertiesFile);
        if(session.getAttribute("maxRThreadCount")!=null){
            this.maxThreadRunning = Integer.parseInt((String)session.getAttribute("maxRThreadCount"));
        }
        if(session.getAttribute("userFilesRoot")!=null){
            this.userFilesRoot = (String) session.getAttribute("userFilesRoot");
            //log.debug("userFilesRoot");
        }
        threadList=(ArrayList<Thread>)session.getServletContext().getAttribute("threadList");
    }

    public ArrayList<Gene> mergeOverlapping(ArrayList<Gene> initialList){
        ArrayList<Gene> mainGenes=new ArrayList<Gene>();
        ArrayList<Gene> rnaGenes=new ArrayList<Gene>();
        ArrayList<Gene> singleExon=new ArrayList<Gene>();
        for(int i=0;i<initialList.size();i++) {
            if (initialList.get(i).getSource().equals("Ensembl")) {
                mainGenes.add(initialList.get(i));
            } else {
                rnaGenes.add(initialList.get(i));
            }
        }
        for(int i=0;i<rnaGenes.size();i++){
            double maxOverlap=0;
            int maxIndex=-1;
            for(int j=0;j<mainGenes.size();j++){
                double overlapPerc=calculateOverlap(rnaGenes.get(i),mainGenes.get(j));
                if(overlapPerc>maxOverlap){
                    maxOverlap=overlapPerc;
                    maxIndex=j;
                }
            }
            if(maxIndex>-1){
                //merge into mainGene at maxIndex
                ArrayList<Transcript> rnaTrans=rnaGenes.get(i).getTranscripts();
                mainGenes.get(maxIndex).addTranscripts(rnaTrans);
            }else{
                //add to main
                if(rnaGenes.get(i).isSingleExon()){
                    singleExon.add(rnaGenes.get(i));
                }else{
                    mainGenes.add(rnaGenes.get(i)); 
                }
            }
        }
        for(int i=0;i<singleExon.size();i++){
            mainGenes.add(singleExon.get(i));
        }
        return mainGenes;
    }
    
    public ArrayList<Gene> mergeAnnotatedOverlapping(ArrayList<Gene> initialList){
        ArrayList<Gene> mainGenes=new ArrayList<Gene>();
        ArrayList<Gene> rnaGenes=new ArrayList<Gene>();
        //ArrayList<Gene> singleExon=new ArrayList<Gene>();
        HashMap<String,Gene> hm=new HashMap<String,Gene>();
        for(int i=0;i<initialList.size();i++){
            if(initialList.get(i).getSource().equals("Ensembl")){
                mainGenes.add(initialList.get(i));
                hm.put(initialList.get(i).getGeneID(),initialList.get(i));
            }else{
                rnaGenes.add(initialList.get(i));
            }
        }
        for(int i=0;i<rnaGenes.size();i++){
            String ens=rnaGenes.get(i).getEnsemblAnnotation();
            if(hm.containsKey(ens)){
                Gene tmpG=hm.get(ens);
                ArrayList<Transcript> tmpTrx=rnaGenes.get(i).getTranscripts();
                tmpG.addTranscripts(tmpTrx);
            }else{
                //add to main
                mainGenes.add(rnaGenes.get(i)); 
            }
        }
        return mainGenes;
    }
    
    public void addHeritDABG(ArrayList<Gene> list,int min,int max,String organism,String chr,int rnaDS_ID,int arrayTypeID,String genomeVer){
        //get all probesets for region with herit and dabg
        if(chr.startsWith("chr")){
            chr=chr.substring(3);
        }
        HashMap probesets=new HashMap();
        String probeQuery="select phd.probeset_id, rd.tissue, phd.herit,phd.dabg "+
                            "from probeset_herit_dabg phd , rnadataset_dataset rd "+
                            "where rd.rna_dataset_id = "+rnaDS_ID+" "+
                            "and phd.dataset_id=rd.dataset_id "+
                            "and phd.genome_id='"+genomeVer+"' "+
                            "and phd.probeset_id in ("+
                                "select s.Probeset_ID "+
                                "from Chromosomes c, Affy_Exon_ProbeSet s "+
                                "where s.chromosome_id = c.chromosome_id "+
                                "and c.name = '"+chr.toUpperCase()+"' "+
                                "and s.genome_id='"+genomeVer+"' "+
                            "and "+
                            "((s.psstart >= "+min+" and s.psstart <="+max+") OR "+
                            "(s.psstop >= "+min+" and s.psstop <= "+max+")) "+
                            "and s.psannotation <> 'transcript' " +
                            "and s.Array_TYPE_ID = "+arrayTypeID+") "+ 
                            "order by phd.probeset_id,rd.tissue";
        
        Connection conn=null;
        try{
            log.debug("herit/DABG SQL\n"+probeQuery);
            conn=pool.getConnection();
            PreparedStatement ps = conn.prepareStatement(probeQuery);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                String probeset=Integer.toString(rs.getInt("PROBESET_ID"));
                double herit=rs.getDouble("herit");
                double dabg=rs.getDouble("dabg");
                String tissue=rs.getString("TISSUE");
                //log.debug("adding"+probeset);
                if(probesets.containsKey(probeset)){
                    HashMap<String,HashMap> phm=(HashMap<String,HashMap>)probesets.get(probeset);
                    HashMap<String,Double> val=new HashMap<String,Double>();
                    val.put("herit", herit);
                    val.put("dabg", dabg);
                    phm.put(tissue, val);
                }else{
                    HashMap<String,HashMap> phm=new HashMap<String,HashMap>();
                    HashMap<String,Double> val=new HashMap<String,Double>();
                    val.put("herit", herit);
                    val.put("dabg", dabg);
                    phm.put(tissue, val);
                    probesets.put(probeset, phm);
                }
            }
            ps.close();
            conn.close();
            //log.debug("HashMap size:"+probesets.size());
        }catch(SQLException e){
            log.error("Error retreiving Herit/DABG.",e);
            System.err.println("Error retreiving Herit/DABG.");
            e.printStackTrace(System.err);
        }finally{
            try {
                    if(conn!=null)
                        conn.close();
                } catch (SQLException ex) {
                }
        }
        if(probesets!=null){
            //fill probeset data for each Gene
            for(int i=0;i<list.size();i++){
                Gene curGene=list.get(i);
                curGene.setHeritDabg(probesets);
            }
        }
        
    }
    //calculate the % of gene1 that overlaps gene2
    public double calculateOverlap(Gene gene1, Gene gene2){
        double ret=0;
        //needs to be on same strand
        if(gene1.getStrand().equals(gene2.getStrand())){
            long gene1S=gene1.getStart();
            long gene1E=gene1.getEnd();
            long gene2S=gene2.getStart();
            long gene2E=gene2.getEnd();
            
            long gene1Len=gene1E-gene1S;
            if(gene1S>gene2S&&gene1S<gene2E){
                long end=gene2E;
                if(gene1E<gene2E){
                    end=gene1E;
                }
                double len=end-gene1S;
                ret=len/gene1Len*100;
            }else if(gene1E>gene2S&&gene1E<gene2E){
                long start=gene2S;
                double len=gene1E-start;
                ret=len/gene1Len*100;
            }else if(gene1S<gene2S&&gene1E>gene2E){
                double len=gene2E-gene2S;
                ret=len/gene1Len*100;
            }
        }
        return ret;
    }
    
    public ArrayList<EQTL> getProbeEQTLs(int min,int max,String chr,int arrayTypeID,ArrayList<String> tissues,String genomeVer){
        ArrayList<EQTL> eqtls=new ArrayList<EQTL>();
        if(genomeVer.equals("rn5")||genomeVer.equals("mm10") ){
            if(chr.startsWith("chr")){
                chr=chr.substring(3);
            }
            //HashMap probesets=new HashMap();
            String qtlQuery="select eq.identifier,eq.lod_score,eq.p_value,eq.fdr,eq.marker,eq.marker_chromosome,eq.marker_mb,eq.lower_limit,eq.upper_limit,eq.tissue "+
                              "from Chromosomes c, Affy_Exon_ProbeSet s "+
                              "left outer join expression_qtls eq on eq.identifier = TO_CHAR (s.probeset_id) "+
                              "where s.chromosome_id = c.chromosome_id "+
                              //"and substr(c.name,1,2) = '"+chr+"'"+
                              "and c.name = '"+chr.toUpperCase()+"'"+
                              "and ((s.psstart >= "+min+" and s.psstart <="+max+") OR "+
                              "(s.psstop >= "+min+" and s.psstop <= "+max+")) "+
                              "and s.psannotation <> 'transcript' " +
                              "and s.Array_TYPE_ID = "+arrayTypeID+" "+
                              "and eq.lod_score>2.5 "+
                              "order by eq.identifier";
            Connection conn=null;
            try{
                log.debug("SQL\n"+qtlQuery);
                conn=pool.getConnection();
                PreparedStatement ps = conn.prepareStatement(qtlQuery);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    String psID=rs.getString(1);
                    double lod=rs.getDouble(2);
                    double pval=rs.getDouble(3);
                    double fdr=rs.getDouble(4);
                    String marker=rs.getString(5);
                    String marker_chr=rs.getString(6);
                    double marker_loc=rs.getDouble(7);
                    double lower=rs.getDouble(8);
                    double upper=rs.getDouble(9);
                    String tissue=rs.getString(10);
                    EQTL eqtl=new EQTL(psID,marker,marker_chr,marker_loc,tissue,lod,pval,fdr,lower,upper);
                    eqtls.add(eqtl);
                    if(!tissues.contains(tissue)){
                        tissues.add(tissue);;
                    }
                }
                ps.close();
                conn.close();
                //log.debug("EQTL size:"+eqtls.size());
                //log.debug("Tissue Size:"+tissues.size());
            }catch(SQLException e){
                log.error("Error retreiving EQTLs.",e);
            }finally{
                try {
                        if(conn!=null)
                            conn.close();
                } catch (SQLException ex) {
                }
            }
        }else{
            
        }
        return eqtls;
    }
    
    public ArrayList<TranscriptCluster> getTransControlledFromEQTLs(int min,int max,String chr,int arrayTypeID,double pvalue,String level){
        if(chr.startsWith("chr")){
            chr=chr.substring(3);
        }
        String tmpRegion=chr+":"+min+"-"+max;
        String curParams="min="+min+",max="+max+",chr="+chr+",arrayid="+arrayTypeID+",pvalue="+pvalue+",level="+level;
        ArrayList<TranscriptCluster> transcriptClusters=new ArrayList<TranscriptCluster>();
        /*boolean run=true;
        if(this.cacheHM.containsKey(tmpRegion)){
            HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
            String testParam=(String)regionHM.get("fromRegionParams");
            if(curParams.equals(testParam)){
                log.debug("\nPrevious results returned-controlled from\n");
                transcriptClusters=(ArrayList<TranscriptCluster>)regionHM.get("fromRegion");
                run=false;
            }
        }
        if(run){*/
            log.debug("\ngenerating new-controlled from\n");
            String qtlQuery="select aep.transcript_cluster_id,c1.name,aep.strand,aep.psstart,aep.psstop,aep.pslevel, s.tissue,lse.pvalue, s.snp_name,c2.name,s.snp_start,s.snp_end "+
                                "from affy_exon_probeset aep " +
                                "left outer join location_specific_eqtl lse on lse.probe_id=aep.probeset_id " +
                    "left outer join snps s on lse.snp_id = s.snp_id " +
                    "left outer join chromosomes c1 on c1.chromosome_id = aep.chromosome_id " +
                    "left outer join chromosomes c2 on c2.chromosome_id = s.chromosome_id "+
                                "where c1.name='"+chr.toUpperCase()+"' "+
                                "and ((aep.psstart >="+min+" and aep.psstart <="+max+") or (aep.psstop>="+min+" and aep.psstop <="+max+")or (aep.psstop<="+min+" and aep.psstop >="+max+")) "+
                                "and aep.psannotation = 'transcript' ";
            if(level.equals("All")){
                qtlQuery=qtlQuery+"and aep.pslevel <> 'ambiguous' ";
            }else{
                qtlQuery=qtlQuery+"and aep.pslevel = '"+level+"' ";
            }
            qtlQuery=qtlQuery+"and aep.array_type_id="+arrayTypeID+" "+
                                "and aep.updatedlocation='Y' "+
                                "and lse.pvalue >= "+(-Math.log10(pvalue))+" "+
                                "order by aep.probeset_id,s.tissue,s.chromosome_id,s.snp_start";
            try(Connection conn=pool.getConnection()){
                log.debug("SQL eQTL FROM QUERY\n"+qtlQuery);
                PreparedStatement ps = conn.prepareStatement(qtlQuery);
                ResultSet rs = ps.executeQuery();
                TranscriptCluster curTC=null;
                while(rs.next()){
                    String tcID=rs.getString(1);
                    //log.debug("process:"+tcID);
                    String tcChr=rs.getString(2);
                    int tcStrand=rs.getInt(3);
                    long tcStart=rs.getLong(4);
                    long tcStop=rs.getLong(5);
                    String tcLevel=rs.getString(6);

                    if(curTC==null||!tcID.equals(curTC.getTranscriptClusterID())){
                        if(curTC!=null){
                            transcriptClusters.add(curTC);
                        }
                        curTC=new TranscriptCluster(tcID,tcChr,Integer.toString(tcStrand),tcStart,tcStop,tcLevel);
                        //log.debug("create transcript cluster:"+tcID);
                    }
                    String tissue=rs.getString(7);
                    double pval=Math.pow(10, (-1*rs.getDouble(8)));
                    String marker_name=rs.getString(9);
                    String marker_chr=rs.getString(10);
                    long marker_start=rs.getLong(11);
                    long marker_end=rs.getLong(12);
                    //double tcLODScore=rs.getDouble(13);
                    curTC.addEQTL(tissue,pval,marker_name,marker_chr,marker_start,marker_end,0);
                }
                if(curTC!=null){
                    transcriptClusters.add(curTC);
                }
                ps.close();
                conn.close();
                //log.debug("Transcript Cluster Size:"+transcriptClusters.size());
                /*if(cacheHM.containsKey(tmpRegion)){
                    HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
                    regionHM.put("fromRegionParams",curParams);        
                    regionHM.put("fromRegion",transcriptClusters);
                }else{
                    HashMap regionHM=new HashMap();
                    regionHM.put("fromRegionParams",curParams);        
                    regionHM.put("fromRegion",transcriptClusters);
                    cacheHM.put(tmpRegion,regionHM);
                    this.cacheList.add(tmpRegion);
                }*/
                //this.fromRegionParams=curParams;
                //this.fromRegion=transcriptClusters;
            }catch(SQLException e){
                log.error("Error retreiving EQTLs.",e);
                e.printStackTrace(System.err);
            }
        //}
        return transcriptClusters;
    }
    
    public String getFolder(int min,int max,String chr,String organism,String genomeVer){
        String folder="";
        if(chr.startsWith("chr")){
            chr=chr.substring(3);
        }
        log.debug("getFolderName:"+organism+"chr"+chr+"_"+min+"_"+max+"_");
        RegionDirFilter rdf=new RegionDirFilter(organism+"chr"+chr+"_"+min+"_"+max+"_");
        log.debug(fullPath + "tmpData/browserCache/"+genomeVer+"/regionData");
        File mainDir=new File(fullPath + "tmpData/browserCache/"+genomeVer+"/regionData");
        File[] list=mainDir.listFiles(rdf);    
        if(list.length>0){
            log.debug("length>0");
            String tmpOutputDir=list[0].getAbsolutePath()+"/";
            int second=tmpOutputDir.lastIndexOf("/",tmpOutputDir.length()-2);
            folder=tmpOutputDir.substring(second+1,tmpOutputDir.length()-1);
                        
        }
        log.debug(folder);
        return folder;
    }
    
    
    public ArrayList<TranscriptCluster> getTransControllingEQTLs(int min,int max,String chr,int arrayTypeID,int RNADatasetID,double pvalue,String level,String organism,String genomeVer,String circosTissue,String circosChr){
        //session.removeAttribute("get");
        ArrayList<TranscriptCluster> transcriptClusters=new ArrayList<TranscriptCluster>();
        ArrayList<TranscriptCluster> beforeFilter=null;
        if(chr.startsWith("chr")){
            chr=chr.substring(3);
        }
        String tmpOutputDir="";
        String folderName="";
        RegionDirFilter rdf=new RegionDirFilter(organism+"chr"+chr+"_"+min+"_"+max+"_");
        File mainDir=new File(fullPath + "tmpData/browserCache/"+genomeVer+"/regionData");
        File[] list=mainDir.listFiles(rdf);    
        if(list.length>0){
                    tmpOutputDir=list[0].getAbsolutePath()+"/";
                    int second=tmpOutputDir.lastIndexOf("/",tmpOutputDir.length()-2);
                    folderName=tmpOutputDir.substring(second+1,tmpOutputDir.length()-1);
                    tmpOutputDir=fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/"+folderName+"/";
        }else{
                String panel="BNLX/SHRH";
                if(organism.equals("Mm")){
                    panel="ILS/ISS";
                }
                this.getRegionData(chr, min, max, panel, organism,genomeVer, RNADatasetID, arrayTypeID, pvalue, false);
                list=mainDir.listFiles(rdf);    
                if(list.length>0){
                            tmpOutputDir=list[0].getAbsolutePath()+"/";
                            int second=tmpOutputDir.lastIndexOf("/",tmpOutputDir.length()-2);
                            folderName=tmpOutputDir.substring(second+1,tmpOutputDir.length()-1);
                            tmpOutputDir=fullPath + "tmpData/browserCache/"+genomeVer+"/regionData/"+folderName+"/";
                }
        }
        
        
        
        circosTissue=circosTissue.replaceAll(";;", ";");
        circosChr=circosChr.replaceAll(";;", ";");
        
        
        String[] levels=level.split(";");
        String tmpRegion=chr+":"+min+"-"+max;
        String curParams="min="+min+",max="+max+",chr="+chr+",arrayid="+arrayTypeID+",pvalue="+pvalue+",level="+level+",org="+organism;
        String curParamsMinusPval="min="+min+",max="+max+",chr="+chr+",arrayid="+arrayTypeID+",level="+level+",org="+organism;
        String curCircosParams="min="+min+",max="+max+",chr="+chr+",arrayid="+arrayTypeID+",pvalue="+pvalue+",level="+level+",org="+organism+",circosTissue="+circosTissue+",circosChr="+circosChr;
        boolean run=true;
        boolean filter=false;
        /*if(this.cacheHM.containsKey(tmpRegion)){
            HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
            String testParam=(String)regionHM.get("controlledRegionParams");
            String testMinusPval="";
            int indPvalue=-1;
            if(testParam!=null){
                indPvalue=testParam.indexOf(",pvalue=")+8;
                if(indPvalue>-1){
                    testMinusPval=testParam.substring(0, indPvalue-8);
                    testMinusPval=testMinusPval+testParam.substring(testParam.indexOf(",",indPvalue));
                }
            }
            //log.debug("\n"+curParamsMinusPval+"\n"+testMinusPval+"\n");
            if(curParams.equals(testParam)){
                //log.debug("\nreturning previous-controlling\n");
                transcriptClusters=(ArrayList<TranscriptCluster>)regionHM.get("controlledRegion");
                run=false;
            }else if(curParamsMinusPval.equals(testMinusPval)){
                //log.debug("\nreturning Filtered\n");
                
                String testPval=testParam.substring(indPvalue,testParam.indexOf(",",indPvalue));
                double testPvalue=Double.parseDouble(testPval);
                if(pvalue<testPvalue){
                    filter=true;
                    run=false;
                    beforeFilter=(ArrayList<TranscriptCluster>)regionHM.get("controlledRegion");
                }
            }
            
            File testF=new File(tmpOutputDir+"TranscriptClusterDetails.txt");
            if(!testF.exists()){
                run=true;
            }
        }
        if(run){*/
            HashMap<String,TranscriptCluster> tmpHM=new HashMap<String,TranscriptCluster>();
            String qtlQuery="select aep.transcript_cluster_id,c2.name,aep.strand,aep.psstart,aep.psstop,aep.pslevel, s.tissue,lse.pvalue, s.snp_name,c.name,s.snp_start,s.snp_end " +
                                "from location_specific_eqtl lse " +
                                "left outer join snps s on s.snp_id=lse.snp_id " +
                                "left outer join chromosomes c on c.chromosome_id=s.chromosome_id " +
                                "left outer join affy_exon_probeset aep on aep.probeset_id=lse.probe_id " +
                                "left outer join chromosomes c2 on c2.chromosome_id=aep.chromosome_id " +
                                "where s.genome_id='"+genomeVer+"' " +
                                "and lse.pvalue between "+(-Math.log10(pvalue))+" and 5.0 " +
                    "and c.name='"+chr.toUpperCase()+"' " +
                    "and (((s.snp_start>="+min+" and s.snp_start<="+max+") or (s.snp_end>="+min+" and s.snp_end<="+max+") or (s.snp_start<="+min+" and s.snp_end>="+min+")) "+
                    " or (s.snp_start=s.snp_end and ((s.snp_start>="+(min-500000)+" and s.snp_start<="+(max+500000)+") or (s.snp_end>="+(min-500000)+" and s.snp_end<="+(max+500000)+") or (s.snp_start<="+(min-500000)+" and s.snp_end>="+(max+500000)+")))) "+
                    "and aep.genome_id='"+genomeVer+"' " +
                    "and aep.updatedlocation='Y' " +
                    "and aep.psannotation='transcript' " +
                    "and aep.array_type_id="+arrayTypeID;


                                //"and ( aep.pslevel='core'  or aep.pslevel='extended'  or aep.pslevel='full' ) \n" +
            if(!level.equals("All")){
                qtlQuery=qtlQuery+" and ( ";
                for(int k=0;k<levels.length;k++){
                    if(k==0){
                        qtlQuery=qtlQuery+"aep.pslevel='"+levels[k]+"' ";
                    }else{
                        qtlQuery=qtlQuery+" or aep.pslevel='"+levels[k]+"' ";
                    }
                }
                qtlQuery=qtlQuery+") ";
            }

//            String qtlQuery="select aep.transcript_cluster_id,c2.name,aep.strand,aep.psstart,aep.psstop,aep.pslevel, s.tissue,lse.pvalue, s.snp_name,c.name,s.snp_start,s.snp_end "+
//                                "from location_specific_eqtl lse, snps s, chromosomes c ,chromosomes c2, affy_exon_probeset aep "+
//                                "where s.snp_id=lse.snp_id "+
//                                "and lse.probe_id=aep.probeset_id "+
//                                "and c2.chromosome_id=aep.chromosome_id "+
//                                "and c.chromosome_id=s.chromosome_id "+
//                                "and lse.pvalue>= "+(-Math.log10(pvalue))+" "+
//                                "and aep.updatedlocation='Y' "+
//                                "and aep.transcript_cluster_id in "+
//                                    "(select aep.transcript_cluster_id "+
//                                    "from location_specific_eqtl lse, snps s, chromosomes c1 , affy_exon_probeset aep "+
//                                    "where s.snp_id=lse.snp_id "+
//                                    "and lse.pvalue>= "+(-Math.log10(pvalue))+" "+
//                                    "and (((s.snp_start>="+min+" and s.snp_start<="+max+") or (s.snp_end>="+min+" and s.snp_end<="+max+") or (s.snp_start<="+min+" and s.snp_end>="+min+")) "+
//                                    " or (s.snp_start=s.snp_end and ((s.snp_start>="+(min-500000)+" and s.snp_start<="+(max+500000)+") or (s.snp_end>="+(min-500000)+" and s.snp_end<="+(max+500000)+") or (s.snp_start<="+(min-500000)+" and s.snp_end>="+(max+500000)+")))) "+
//                                    "and s.chromosome_id=c1.chromosome_id "+
//                                    "and s.organism ='"+organism+"' "+
//                                    "and c1.name='"+chr.toUpperCase()+"' "+
//                                    "and aep.updatedlocation='Y' "+
//                                    "and lse.probe_id=aep.probeset_id ";
//                                if(!level.equals("All")){
//                                    qtlQuery=qtlQuery+" and ( ";
//                                    for(int k=0;k<levels.length;k++){
//                                        if(k==0){
//                                            qtlQuery=qtlQuery+"aep.pslevel='"+levels[k]+"' ";
//                                        }else{
//                                            qtlQuery=qtlQuery+" or aep.pslevel='"+levels[k]+"' ";
//                                        }
//                                    }
//                                    qtlQuery=qtlQuery+") ";
//                                    //qtlQuery=qtlQuery+"and aep.pslevel='"+level+"' ";
//                                }/*else{
//                                    qtlQuery=qtlQuery+"and aep.pslevel<>'ambiguous' ";
//                                }*/
//                                qtlQuery=qtlQuery+"and aep.psannotation='transcript' "+
//                                "and aep.array_type_id="+arrayTypeID+") "+
//                                "order by aep.transcript_cluster_id, s.tissue";
            String qtlQuery2="select aep.transcript_cluster_id,c2.name,aep.strand,aep.psstart,aep.psstop,aep.pslevel, s.tissue,lse.pvalue, s.snp_name,c.name,s.snp_start,s.snp_end " +
                                "from location_specific_eqtl lse " +
                                "left outer join snps s on s.snp_id=lse.snp_id " +
                                "left outer join chromosomes c on c.chromosome_id=s.chromosome_id " +
                                "left outer join affy_exon_probeset aep on aep.probeset_id=lse.probe_id " +
                                "left outer join chromosomes c2 on c2.chromosome_id=aep.chromosome_id " +
                                "where  s.genome_id='"+genomeVer+"' " +
                                "and lse.pvalue between 1.0 and "+(-Math.log10(pvalue))+" " +
                                "and c.name='"+chr.toUpperCase()+"' " +
                                "and (((s.snp_start>="+min+" and s.snp_start<="+max+") or (s.snp_end>="+min+" and s.snp_end<="+max+") or (s.snp_start<="+min+" and s.snp_end>="+min+")) "+
                                " or (s.snp_start=s.snp_end and ((s.snp_start>="+(min-500000)+" and s.snp_start<="+(max+500000)+") or (s.snp_end>="+(min-500000)+" and s.snp_end<="+(max+500000)+") or (s.snp_start<="+(min-500000)+" and s.snp_end>="+(max+500000)+")))) "+
                                "and aep.genome_id='"+genomeVer+"' "+
                                "and aep.updatedlocation='Y' " +
                                "and aep.psannotation='transcript' " +
                                "and aep.array_type_id="+arrayTypeID+" ";

            if(!level.equals("All")){
                qtlQuery2=qtlQuery2+" and ( ";
                for(int k=0;k<levels.length;k++){
                    if(k==0){
                        qtlQuery2=qtlQuery2+"aep.pslevel='"+levels[k]+"' ";
                    }else{
                        qtlQuery2=qtlQuery2+" or aep.pslevel='"+levels[k]+"' ";
                    }
                }
                qtlQuery2=qtlQuery2+") ";
            }
            //qtlQuery2=qtlQuery2+"order by s.tissue";
//            String qtlQuery2="select aep.transcript_cluster_id,c2.name,aep.strand,aep.psstart,aep.psstop,aep.pslevel, s.tissue,lse.pvalue, s.snp_name,c.name,s.snp_start,s.snp_end "+//,eq.LOD_SCORE "+
//                                "from location_specific_eqtl lse, snps s, chromosomes c ,chromosomes c2, affy_exon_probeset aep "+//, expression_qtls eq "+
//                                "where s.snp_id=lse.snp_id "+
//                                "and lse.pvalue< "+(-Math.log10(pvalue))+" "+
//                                //"and substr(c.name,1,2)='"+chr+"' "+
//                                "and c.name='"+chr.toUpperCase()+"' "+
//                                "and (((s.snp_start>="+min+" and s.snp_start<="+max+") or (s.snp_end>="+min+" and s.snp_end<="+max+") or (s.snp_start<="+min+" and s.snp_end>="+max+")) "+
//                                " or (s.snp_start=s.snp_end and ((s.snp_start>="+(min-500000)+" and s.snp_start<="+(max+500000)+") or (s.snp_end>="+(min-500000)+" and s.snp_end<="+(max+500000)+") or (s.snp_start<="+(min-500000)+" and s.snp_end>="+(max+500000)+")))) "+
//                                "and s.organism ='"+organism+"' "+
//                                "and lse.probe_id=aep.probeset_id ";
//                                if(!level.equals("All")){
//                                    qtlQuery2=qtlQuery2+" and ( ";
//                                    for(int k=0;k<levels.length;k++){
//                                        if(k==0){
//                                            qtlQuery2=qtlQuery2+"aep.pslevel='"+levels[k]+"' ";
//                                        }else{
//                                            qtlQuery2=qtlQuery2+" or aep.pslevel='"+levels[k]+"' ";
//                                        }
//                                    }
//                                    qtlQuery2=qtlQuery2+") ";
//                                }
//                                qtlQuery2=qtlQuery2+"and aep.psannotation='transcript' "+
//                                "and aep.array_type_id="+arrayTypeID+" "+
//                                "and aep.updatedlocation='Y' "+
//                                "and s.chromosome_id=c.chromosome_id "+
//                                "and c2.chromosome_id=aep.chromosome_id "+
//                                "order by aep.transcript_cluster_id,s.tissue,aep.chromosome_id,aep.psstart";
            try(Connection conn=pool.getConnection()){
                log.debug("SQL eQTL FROM QUERY\n"+qtlQuery);
                PreparedStatement ps = conn.prepareStatement(qtlQuery);
                ResultSet rs = ps.executeQuery();
                eQTLRegions=new HashMap();
                TranscriptCluster curTC=null;
                while(rs.next()){
                    String tcID=rs.getString(1);
                    //log.debug("process:"+tcID);
                    String tcChr=rs.getString(2);
                    int tcStrand=rs.getInt(3);
                    long tcStart=rs.getLong(4);
                    long tcStop=rs.getLong(5);
                    String tcLevel=rs.getString(6);

                    if(tmpHM.containsKey(tcID)){
                        curTC=tmpHM.get(tcID);
                    }else{
                        curTC=new TranscriptCluster(tcID,tcChr,Integer.toString(tcStrand),tcStart,tcStop,tcLevel);
                        tmpHM.put(tcID,curTC);
                    }
                    String tissue=rs.getString(7);
                    //log.debug("tissue:"+tissue+":");
                    double pval=Math.pow(10, (-1*rs.getDouble(8)));
                    String marker_name=rs.getString(9);
                    String marker_chr=rs.getString(10);
                    long marker_start=rs.getLong(11);
                    long marker_end=rs.getLong(12);
                    //double tcLODScore=rs.getDouble(13);
                    if(marker_chr.equals(chr) && ((marker_start>=min && marker_start<=max) || (marker_end>=min && marker_end<=max) || (marker_start<=min && marker_end>=max)) ){
                        curTC.addRegionEQTL(tissue,pval,marker_name,marker_chr,marker_start,marker_end,-1);
                        DecimalFormat df=new DecimalFormat("#,###");
                        String eqtl="chr"+marker_chr+":"+df.format(marker_start)+"-"+df.format(marker_end);
                        if(!eQTLRegions.containsKey(eqtl)){
                            eQTLRegions.put(eqtl, 1);
                        }
                    }else{
                        curTC.addEQTL(tissue,pval,marker_name,marker_chr,marker_start,marker_end,-1);
                    }
                }
                ps.close();
                log.debug("done");
                
                if(tmpHM.size()==0){
                    String snpQ="select * from snps s,chromosomes c where "+
                            "((s.snp_start>="+min+" and s.snp_start<="+max+") or (s.snp_end>="+min+" and s.snp_end<="+max+") or (s.snp_start<="+min+" and s.snp_end>="+max+")) "+
                            "and s.chromosome_id=c.chromosome_id "+
                            "and s.genome_id ='"+genomeVer+"' "+
                            //"and substr(c.name,1,2)='"+chr+"' ";
                            "and c.name='"+chr.toUpperCase()+"' ";
                    ps = conn.prepareStatement(snpQ);
                    rs = ps.executeQuery();
                    int snpcount=0;
                    while(rs.next()){
                        snpcount++;
                    }
                    ps.close();
                    if(snpcount>0){
                        //for now don't do anything later we can try adjusting the p-value
                    }else{
                        session.setAttribute("getTransControllingEQTL","This region does not overlap with any markers used in the eQTL calculations.  You should expand the region to view eQTLs.");
                    }
                    
                }else{
                    log.debug("Query2:"+qtlQuery2);
                    ps = conn.prepareStatement(qtlQuery2);
                    rs = ps.executeQuery();

                    while(rs.next()){
                        String tcID=rs.getString(1);
                        String tissue=rs.getString(7);
                        double pval=Math.pow(10, (-1*rs.getDouble(8)));
                        String marker_name=rs.getString(9);
                        String marker_chr=rs.getString(10);
                        long marker_start=rs.getLong(11);
                        long marker_end=rs.getLong(12);
                        //double tcLODScore=rs.getDouble(13);
                        if(tmpHM.containsKey(tcID)){
                            TranscriptCluster tmpTC=(TranscriptCluster)tmpHM.get(tcID);
                            tmpTC.addRegionEQTL(tissue,pval,marker_name,marker_chr,marker_start,marker_end,-1);
                        }

                    }
                    ps.close();
                }
                conn.close();
                Set keys=tmpHM.keySet();
                Iterator itr=keys.iterator();
                try{
                    BufferedWriter out=new BufferedWriter(new FileWriter(new File(tmpOutputDir+"transcluster.txt")));
                    while(itr.hasNext()){
                        TranscriptCluster tmpC=(TranscriptCluster)tmpHM.get(itr.next().toString());
                        if(tmpC!=null){
                            if(tmpC.getTissueRegionEQTLs().size()>0){
                                transcriptClusters.add(tmpC);
                                String line=tmpC.getTranscriptClusterID()+"\t"+tmpC.getChromosome()+"\t"+tmpC.getStart()+"\t"+tmpC.getEnd()+"\t"+tmpC.getStrand()+"\n";
                                out.write(line);
                                
                            }
                        }
                    }
                    out.flush();
                    out.close();
                }catch(IOException e){
                    log.error("I/O Exception trying to output transcluster.txt file.",e);
                    session.setAttribute("getTransControllingEQTL","Error retreiving eQTLs.  Please try again later.  The administrator has been notified of the problem.");
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in GeneDataTools.getTransControllingEQTLS");
                    myAdminEmail.setContent("There was an error while running getTransControllingEQTLS.\nI/O Exception trying to output transcluster.txt file.",e);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }
                HashMap<String,String> source=getGenomeVersionSource(genomeVer);
                String ensemblPath=source.get("ensembl");
                File ensPropertiesFile = new File(ensemblDBPropertiesFile);
                Properties myENSProperties = new Properties();
                String ensHost="";
                String ensPort="";
                String ensUser="";
                String ensPassword="";
                try{
                    myENSProperties.load(new FileInputStream(ensPropertiesFile));
                    ensHost=myENSProperties.getProperty("HOST");
                    ensPort=myENSProperties.getProperty("PORT");
                    ensUser=myENSProperties.getProperty("USER");
                    ensPassword=myENSProperties.getProperty("PASSWORD");
                }catch(IOException e){
                    log.error("I/O Exception trying to read properties file.",e);
                    session.setAttribute("getTransControllingEQTL","Error retreiving eQTLs.  Please try again later.  The administrator has been notified of the problem.");
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in GeneDataTools.getTransControllingEQTLS");
                    myAdminEmail.setContent("There was an error while running getTransControllingEQTLS.\nI/O Exception trying to read properties file.",e);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }

                boolean error=false;
                String[] perlArgs = new String[9];
                perlArgs[0] = "perl";
                perlArgs[1] = perlDir + "writeGeneIDs.pl";
                perlArgs[2] = tmpOutputDir+"transcluster.txt";
                perlArgs[3] = tmpOutputDir+"TC_to_Gene.txt";
                if (organism.equals("Rn")) {
                    perlArgs[4] = "Rat";
                } else if (organism.equals("Mm")) {
                    perlArgs[4] = "Mouse";
                }
                perlArgs[5] = ensHost;
                perlArgs[6] = ensPort;
                perlArgs[7] = ensUser;
                perlArgs[8] = ensPassword;


                //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list
                String[] envVar=perlEnvVar.split(",");

                for (int i = 0; i < envVar.length; i++) {
                    if(envVar[i].contains("/ensembl")){
                        envVar[i]=envVar[i].replaceFirst("/ensembl", "/"+ensemblPath);
                    }
                    log.debug(i + " EnvVar::" + envVar[i]);
                }


                //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
                myExec_session = new ExecHandler(perlDir, perlArgs, envVar, tmpOutputDir+"toGeneID");
                boolean exception=false;
                try {

                    myExec_session.runExec();

                } catch (ExecException e) {
                    exception=true;
                    error=true;
                    log.error("In Exception of run writeGeneIDs.pl Exec_session", e);
                    session.setAttribute("getTransControllingEQTL","Error retreiving eQTLs.  Please try again later.  The administrator has been notified of the problem.");
                    setError("Running Perl Script to match Transcript Clusters to Genes.");
                    Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+")\n\n"+myExec_session.getErrors());
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }

                String errors=myExec_session.getErrors();
                if(!exception && errors!=null && !(errors.equals(""))){
                    error=true;
                    Email myAdminEmail = new Email();
                    session.setAttribute("getTransControllingEQTL","Error retreiving eQTLs.  Please try again later.  The administrator has been notified of the problem.");
                    myAdminEmail.setSubject("Exception thrown in Exec_session");
                    myAdminEmail.setContent("There was an error while running "
                            + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+
                            ")\n\n"+errors);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                        try {
                            myAdminEmail.sendEmailToAdministrator("");
                        } catch (Exception mailException1) {
                            //throw new RuntimeException();
                        }
                    }
                }
                if(!error){
                    try{
                        log.debug("Read TC_to_Gene");
                        BufferedReader in = new BufferedReader(new FileReader(new File(tmpOutputDir+"TC_to_Gene.txt")));
                        while(in.ready()){
                            String line=in.readLine();
                            String[] tabs=line.split("\t");
                            String tcID=tabs[0];
                            String ensID=tabs[1];
                            String geneSym=tabs[2];
                            String sStart=tabs[3];
                            String sEnd=tabs[4];
                            String sOverlap=tabs[5];
                            String sOverlapG=tabs[6];
                            String description="";
                            if(tabs.length>7){
                                description=tabs[7];
                            }
                            if(tmpHM.containsKey(tcID)){
                                TranscriptCluster tmpTC=(TranscriptCluster)tmpHM.get(tcID);
                                tmpTC.addGene(ensID,geneSym,sStart,sEnd,sOverlap,sOverlapG,description);
                            }
                        }
                        in.close();
                        log.debug("write transcriptclusterdetails.txt");
                        BufferedWriter out= new BufferedWriter(new FileWriter(new File(tmpOutputDir+"TranscriptClusterDetails.txt")));
                        for(int i=0;i<transcriptClusters.size();i++){
                            TranscriptCluster tc=transcriptClusters.get(i);
                            HashMap hm=tc.getTissueRegionEQTLs();
                            Set key=hm.keySet();
                            if(key!=null){
                                Object[] tissue=key.toArray();
                                for(int j=0;j<tissue.length;j++){
                                    String line="";
                                    ArrayList<EQTL> tmpEQTLArr=(ArrayList<EQTL>)hm.get(tissue[j].toString());
                                    if(tmpEQTLArr!=null && tmpEQTLArr.size()>0){
                                        EQTL tmpEQTL=tmpEQTLArr.get(0);
                                        if(tmpEQTL.getMarkerChr().equals(chr) && 
                                                ((tmpEQTL.getMarker_start()>=min && tmpEQTL.getMarker_start()<=max) || 
                                                (tmpEQTL.getMarker_end()>=min && tmpEQTL.getMarker_end()<=max) || 
                                                (tmpEQTL.getMarker_start()<=min && tmpEQTL.getMarker_end()>=max))
                                                ){
                                            line=tmpEQTL.getMarkerName()+"\t"+tmpEQTL.getMarkerChr()+"\t"+tmpEQTL.getMarker_start();
                                            line=line+"\t"+tc.getTranscriptClusterID()+"\t"+tc.getChromosome()+"\t"+tc.getStart()+"\t"+tc.getEnd();
                                            String tmpGeneSym=tc.getGeneSymbol();
                                            if(tmpGeneSym==null||tmpGeneSym.equals("")){
                                                tmpGeneSym=tc.getGeneID();
                                            }
                                            if(tmpGeneSym==null||tmpGeneSym.equals("")){
                                                tmpGeneSym=tc.getTranscriptClusterID();
                                            }
                                            line=line+"\t"+tmpGeneSym+"\t"+tissue[j].toString()+"\t"+tmpEQTL.getNegLogPVal()+"\n";
                                            out.write(line);
                                        }
                                    }
                                }
                            }

                        }
                        out.close();
                        log.debug("Done-transcript cluster details.");
                    }catch(IOException e){
                        log.error("Error reading Gene - Transcript IDs.",e);
                        session.setAttribute("getTransControllingEQTL","Error retreiving eQTLs.  Please try again later.  The administrator has been notified of the problem.");
                        Email myAdminEmail = new Email();
                        myAdminEmail.setSubject("Exception thrown in GeneDataTools.getTransControllingEQTLS");
                        myAdminEmail.setContent("There was an error while running getTransControllingEQTLS.\nI/O Exception trying to read Gene - Transcript IDs file.",e);
                        try {
                            myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                        } catch (Exception mailException) {
                            log.error("error sending message", mailException);
                        }
                    }
                    

                }
                log.debug("Transcript Cluster Size:"+transcriptClusters.size());
                //this.controlledRegionParams=curParams;
                //this.controlledRegion=transcriptClusters;
                /*if(cacheHM.containsKey(tmpRegion)){
                    HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
                    regionHM.put("controlledRegionParams",curParams);        
                    regionHM.put("controlledRegion",transcriptClusters);
                }else{
                    HashMap regionHM=new HashMap();
                    regionHM.put("controlledRegionParams",curParams);        
                    regionHM.put("controlledRegion",transcriptClusters);
                    cacheHM.put(tmpRegion,regionHM);
                    this.cacheList.add(tmpRegion);
                }*/
            }catch(SQLException e){
                log.error("Error retreiving EQTLs.",e);
                session.setAttribute("getTransControllingEQTL","Error retreiving eQTLs.  Please try again later.  The administrator has been notified of the problem.");
                e.printStackTrace(System.err);
                Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in GeneDataTools.getTransControllingEQTLS");
                    myAdminEmail.setContent("There was an error while running getTransControllingEQTLS.\n SQLException getting transcript clusters.",e);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                    }
            }
        /*}else if(filter){//don't need to rerun just filter.
            log.debug("transcript controlling Filtering");
            String[] includedTissues=circosTissue.split(";");
            for(int i=0;i<includedTissues.length;i++){
                if(includedTissues[i].equals("Brain")){
                    includedTissues[i]="Whole Brain";
                }else if(includedTissues[i].equals("BAT")){
                    includedTissues[i]="Brown Adipose";
                }
            }
            for(int i=0;i<beforeFilter.size();i++){
                TranscriptCluster tc=beforeFilter.get(i);
                boolean include=false;
                for(int j=0;j<includedTissues.length&&!include;j++){
                    ArrayList<EQTL> regionQTL=tc.getTissueRegionEQTL(includedTissues[j]);
                    
                    if(regionQTL!=null){
                            EQTL regQTL=regionQTL.get(0);
                            if(regQTL.getPVal()<=pvalue){
                                    include=true;
                            }
                    }
                }
                if(include){
                    transcriptClusters.add(tc);
                }
            }
        }*/
        run=true;
        /*if(this.cacheHM.containsKey(tmpRegion)){
            HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
            String testParam=(String)regionHM.get("controlledCircosRegionParams");
            if(curCircosParams.equals(testParam)){
                //log.debug("\nreturning previous-circos\n");
                run=false;
            }
        }*/
        
        //File test=new File(tmpOutputDir.substring(0,tmpOutputDir.length()-1)+"/circos"+Double.toString(-Math.log10(pvalue)));
        File test=new File(tmpOutputDir.substring(0,tmpOutputDir.length()-1)+"/circos"+pvalue);
        if(!test.exists()){
            log.debug("\ngenerating new-circos\n");
            
            //run circos scripts
            boolean errorCircos=false;
            String[] perlArgs = new String[7];
            perlArgs[0] = "perl";
            perlArgs[1] = perlDir + "callCircosReverse.pl";
            perlArgs[2] = Double.toString(-Math.log10(pvalue));
            perlArgs[3] = organism;
            perlArgs[4] = tmpOutputDir.substring(0,tmpOutputDir.length()-1);
            perlArgs[5] = circosTissue;
            perlArgs[6] = circosChr;
            //remove old circos directory
            double cutoff=pvalue;
            String circosDir=tmpOutputDir+"circos"+cutoff;
            File circosFile=new File(circosDir);
            if(circosFile.exists()){
                try{
                    myFH.deleteAllFilesPlusDirectory(circosFile);
                }catch(Exception e){
                    log.error("Error trying to delete circos directory\n",e);
                }
            }

            //set environment variables so you can access oracle pulled from perlEnvVar session variable which is a comma separated list

            /*for (int i = 0; i < perlArgs.length; i++) {
                log.debug(i + " perlArgs::" + perlArgs[i]);
            }*/
            String[] envVar=perlEnvVar.split(",");
            /*for (int i = 0; i < envVar.length; i++) {
                log.debug(i + " EnvVar::" + envVar[i]);
            }*/


            //construct ExecHandler which is used instead of Perl Handler because environment variables were needed.
            myExec_session = new ExecHandler(perlDir, perlArgs, envVar, tmpOutputDir+"circos_"+pvalue);
            
            try {

                myExec_session.runExec();
                /*if(cacheHM.containsKey(tmpRegion)){
                    HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
                    regionHM.put("controlledCircosRegionParams",curCircosParams);        
                }else{
                    HashMap regionHM=new HashMap();
                    regionHM.put("controlledCircosRegionParams",curCircosParams);        
                    cacheHM.put(tmpRegion,regionHM);
                    this.cacheList.add(tmpRegion);
                }*/
                //this.controlledCircosRegionParams=curCircosParams;
            } catch (ExecException e) {
                //error=true;
                log.error("In Exception of run callCircosReverse.pl Exec_session", e);
                session.setAttribute("getTransControllingEQTLCircos","Error running Circos.  Unable to generate Circos image.  Please try again later.  The administrator has been notified of the problem.");
                setError("Running Perl Script to match create circos plot.");
               /* Email myAdminEmail = new Email();
                myAdminEmail.setSubject("Exception thrown in Exec_session");
                myAdminEmail.setContent("There was an error while running "
                        + perlArgs[1] + " (" + perlArgs[2] +" , "+perlArgs[3]+" , "+perlArgs[4]+")\n\n"+myExec_session.getErrors());
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                    try {
                        myAdminEmail.sendEmailToAdministrator("");
                    } catch (Exception mailException1) {
                        //throw new RuntimeException();
                    }
                }*/
            }
        }
        
        return transcriptClusters;
    }
    
    public ArrayList<SmallNonCodingRNA> getSmallNonCodingRNA(int min,int max,String chr,int rnaDatasetID,String organism){
        //session.removeAttribute("get");
        HashMap smncID=new HashMap();
        ArrayList<SmallNonCodingRNA> smncRNA=new ArrayList<SmallNonCodingRNA>();
        if(chr.startsWith("chr")){
            chr=chr.substring(3);
        }
        String tmpRegion=chr+":"+min+"-"+max;
        String curParams="min="+min+",max="+max+",chr="+chr+",org="+organism;
  
        /*boolean run=true;
        if(this.cacheHM.containsKey(tmpRegion)){
            HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
            String testParam=(String)regionHM.get("smallNonCodingParams");
            if(curParams.equals(testParam)){
                //log.debug("\nreturning previous-controlling\n");
                smncRNA=(ArrayList<SmallNonCodingRNA>)regionHM.get("smallNonCoding");
                run=false;
            }
        }
        if(run){*/
            HashMap tmpHM=new HashMap();

            String smncQuery="Select rsn.rna_smnc_id,rsn.feature_start,rsn.feature_stop,rsn.sample_count,rsn.total_reads,rsn.strand,rsn.reference_seq,c.name "+
                             "from rna_sm_noncoding rsn, chromosomes c "+ 
                             "where c.chromosome_id=rsn.chromosome_id "+
                             "and c.name = '"+chr.toUpperCase()+"' "+
                             "and rsn.rna_dataset_id="+rnaDatasetID+" "+
                             "and ((rsn.feature_start>="+min+" and rsn.feature_start<="+max+") OR (rsn.feature_stop>="+min+" and rsn.feature_stop<="+max+") OR (rsn.feature_start<="+min+" and rsn.feature_stop>="+max+")) ";

            String smncSeqQuery="select s.* from rna_smnc_seq s "+
                                "where s.rna_smnc_id in ("+
                                "select rsn.rna_smnc_id "+
                                "from rna_sm_noncoding rsn, chromosomes c "+ 
                                "where c.chromosome_id=rsn.chromosome_id "+
                                "and  c.name =  '"+chr.toUpperCase()+"' "+
                                "and rsn.rna_dataset_id="+rnaDatasetID+" "+
                                "and ((rsn.feature_start>="+min+" and rsn.feature_start<="+max+") OR (rsn.feature_stop>="+min+" and rsn.feature_stop<="+max+") OR (rsn.feature_start<="+min+" and rsn.feature_stop>="+max+")) "+
                                ")";
                                
            String smncAnnotQuery="select a.rna_smnc_annot_id,a.rna_smnc_id,a.annotation,s.shrt_name from rna_smnc_annot a,rna_annot_src s "+
                                "where s.rna_annot_src_id=a.source_id "+
                                "and a.rna_smnc_id in ("+
                                "select rsn.rna_smnc_id "+
                                "from rna_sm_noncoding rsn, chromosomes c "+ 
                                "where c.chromosome_id=rsn.chromosome_id "+
                                "and  c.name =  '"+chr.toUpperCase()+"' "+
                                "and rsn.rna_dataset_id="+rnaDatasetID+" "+
                                "and ((rsn.feature_start>="+min+" and rsn.feature_start<="+max+") OR (rsn.feature_stop>="+min+" and rsn.feature_stop<="+max+") OR (rsn.feature_start<="+min+" and rsn.feature_stop>="+max+")) "+
                                ")";
           
           String smncVarQuery="select v.* from rna_smnc_variant v "+
                                "where v.rna_smnc_id in ("+
                                "select rsn.rna_smnc_id "+
                                "from rna_sm_noncoding rsn, chromosomes c "+ 
                                "where c.chromosome_id=rsn.chromosome_id "+
                                "and  c.name =  '"+chr.toUpperCase()+"' "+
                                "and rsn.rna_dataset_id="+rnaDatasetID+" "+
                                "and ((rsn.feature_start>="+min+" and rsn.feature_start<="+max+") OR (rsn.feature_stop>="+min+" and rsn.feature_stop<="+max+") OR (rsn.feature_start<="+min+" and rsn.feature_stop>="+max+")) "+
                                ")";

            try(Connection conn=pool.getConnection()){
                log.debug("SQL smnc FROM QUERY\n"+smncQuery);
                PreparedStatement ps = conn.prepareStatement(smncQuery);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    int id=rs.getInt(1);
                    int start=rs.getInt(2);
                    int stop=rs.getInt(3);
                    String smplCount=rs.getString(4);
                    int total=rs.getInt(5);
                    int strand=rs.getInt(6);
                    String ref=rs.getString(7);
                    String chrom=rs.getString(8);
                    SmallNonCodingRNA tmpSmnc=new SmallNonCodingRNA(id,start,stop,chrom,ref,strand,total);
                    smncRNA.add(tmpSmnc);
                    smncID.put(id,tmpSmnc);
                }
                ps.close();
                log.debug("SQL smncSeq FROM QUERY\n"+smncSeqQuery);
                ps = conn.prepareStatement(smncSeqQuery);
                rs = ps.executeQuery();
                while(rs.next()){
                    int id=rs.getInt(1);
                    int smID=rs.getInt(2);
                    String seq=rs.getString(3);
                    int readCount=rs.getInt(4);
                    int unique=rs.getInt(5);
                    int offset=rs.getInt(6);
                    int bnlx=rs.getInt(7);
                    int shrh=rs.getInt(8);
                    HashMap<String,Integer> match=new HashMap<String,Integer>();
                    match.put("BNLX", bnlx);
                    match.put("SHRH", shrh);
                    RNASequence tmpSeq=new RNASequence(id,seq,readCount,unique,offset,match);
                    if(smncID.containsKey(smID)){
                        SmallNonCodingRNA tmp=(SmallNonCodingRNA)smncID.get(smID);
                        tmp.addSequence(tmpSeq);
                    }
                }
                ps.close();
                log.debug("SQL smncAnnot FROM QUERY\n"+smncAnnotQuery);
                ps = conn.prepareStatement(smncAnnotQuery);
                rs = ps.executeQuery();
                while(rs.next()){
                    int id=rs.getInt(1);
                    int smID=rs.getInt(2);
                    String annot=rs.getString(3);
                    String src=rs.getString(4);
                    Annotation tmpAnnot=new Annotation(id,src,annot,"smnc");
                    if(smncID.containsKey(smID)){
                        //log.debug("adding:"+smID);
                        SmallNonCodingRNA tmp=(SmallNonCodingRNA)smncID.get(smID);
                        tmp.addAnnotation(tmpAnnot);
                    }else{
                        log.debug("ID not found:"+smID);
                    }
                }
                ps.close();
                ps = conn.prepareStatement(smncVarQuery);
                rs = ps.executeQuery();
                while(rs.next()){
                    int id=rs.getInt(1);
                    int smID=rs.getInt(2);
                    int start=rs.getInt(3);
                    int stop=rs.getInt(4);
                    String refSeq=rs.getString(5);
                    String strainSeq=rs.getString(6);
                    String type=rs.getString(7);
                    String strain=rs.getString(8);
                    SequenceVariant tmpVar=new SequenceVariant(id,start,stop,refSeq,strainSeq,type,strain);
                    if(smncID.containsKey(smID)){
                        SmallNonCodingRNA tmp=(SmallNonCodingRNA)smncID.get(smID);
                        tmp.addVariant(tmpVar);
                    }
                }
                ps.close();
                /*if(cacheHM.containsKey(tmpRegion)){
                    HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
                    regionHM.put("smallNonCodingParams",curParams); 
                    regionHM.put("smallNonCoding",smncRNA);
                }else{
                    HashMap regionHM=new HashMap();
                    regionHM.put("smallNonCodingParams",curParams); 
                    regionHM.put("smallNonCoding",smncRNA);       
                    cacheHM.put(tmpRegion,regionHM);
                    this.cacheList.add(tmpRegion);
                }*/
                conn.close();
            
            }catch(SQLException e){
                log.error("Error retreiving SMNCs.",e);
                //session.setAttribute("getTransControllingEQTL","Error retreiving eQTLs.  Please try again later.  The administrator has been notified of the problem.");
                e.printStackTrace(System.err);
                Email myAdminEmail = new Email();
                    myAdminEmail.setSubject("Exception thrown in GeneDataTools.getSmallNonCodingRNA");
                    myAdminEmail.setContent("There was an error while running getSmallNonCodingRNA.\n",e);
                    try {
                        myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                    } catch (Exception mailException) {
                        log.error("error sending message", mailException);
                    }
            }

        return smncRNA;
    }
    
    public ArrayList<String> getEQTLRegions(){
        ArrayList<String> ret=new ArrayList<String>();
        Set tmp=this.eQTLRegions.keySet();
        Iterator itr=tmp.iterator();
        while(itr.hasNext()){
            String key=itr.next().toString();
            ret.add(key);
        }
        return ret;
    }
    
    public ArrayList<BQTL> getBQTLs(int min,int max,String chr,String organism,String genomeVer){
        if(chr.startsWith("chr")){
            chr=chr.substring(3);
        }
        String tmpRegion=chr+":"+min+"-"+max;
        String curParams="min="+min+",max="+max+",chr="+chr+",org="+organism;
        ArrayList<BQTL> bqtl=new ArrayList<BQTL>();
        session.removeAttribute("getBQTLsERROR");
        boolean run=true;

            String query="select pq.*,c.name from public_qtls pq, chromosomes c "+
                            "where pq.genome_id='"+genomeVer+"' "+
                            "and ((pq.qtl_start>="+min+" and pq.qtl_start<="+max+") or (pq.qtl_end>="+min+" and pq.qtl_end<="+max+") or (pq.qtl_start<="+min+" and pq.qtl_end>="+max+")) "+
                            "and c.name='"+chr.toUpperCase()+"' "+ 
                            "and c.chromosome_id=pq.chromosome";
            try{ 
            try(Connection conn=pool.getConnection()){
                log.debug("SQL eQTL FROM QUERY\n"+query);
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                while(rs.next()){
                    String id=Integer.toString(rs.getInt(1));
                    String mgiID=rs.getString(2);
                    String rgdID=rs.getString(3);
                    String symbol=rs.getString(5);
                    String name=rs.getString(6);
                    double lod=rs.getDouble(8);
                    double pvalue=rs.getDouble(9);
                    String trait=rs.getString(10);
                    String subTrait=rs.getString(11);
                    String traitMethod=rs.getString(12);
                    String phenotype=rs.getString(13);
                    String diseases=rs.getString(14);
                    String rgdRef=rs.getString(15);
                    String pubmedRef=rs.getString(16);
                    String relQTLs=rs.getString(18);
                    String candidGene=rs.getString(17);
                    long start=rs.getLong(19);
                    long stop=rs.getLong(20);
                    String mapMethod=rs.getString(21);
                    String chromosome=rs.getString(23);
                    BQTL tmpB=new BQTL(id,mgiID,rgdID,symbol,name,trait,subTrait,traitMethod,phenotype,diseases,rgdRef,pubmedRef,mapMethod,relQTLs,candidGene,lod,pvalue,start,stop,chromosome);
                    bqtl.add(tmpB);
                }
                ps.close();
                conn.close();
                /*if(cacheHM.containsKey(tmpRegion)){
                    HashMap regionHM=(HashMap)cacheHM.get(tmpRegion);
                    regionHM.put("bqtlParams",curParams);        
                    regionHM.put("bqtl",bqtl);
                }else{
                    HashMap regionHM=new HashMap();
                    regionHM.put("bqtlParams",curParams);        
                    regionHM.put("controlledRegion",bqtl);
                    cacheHM.put(tmpRegion,regionHM);
                    this.cacheList.add(tmpRegion);
                }*/
            }catch(SQLException e){
                log.error("Error retreiving bQTLs.",e);
                e.printStackTrace(System.err);
                session.setAttribute("getBQTLsERROR","Error retreiving region bQTLs.  Please try again later.  The administrator has been notified of the problem.");
                 Email myAdminEmail = new Email();
                 myAdminEmail.setSubject("Exception thrown in GeneDataTools.getBQTLs");
                 myAdminEmail.setContent("There was an error while running getBQTLs.",e);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                }
            }
            }catch(Exception er){
                er.printStackTrace(System.err);
                session.setAttribute("getBQTLsERROR","Error retreiving region bQTLs.  Please try again later.  The administrator has been notified of the problem.");
                 Email myAdminEmail = new Email();
                 myAdminEmail.setSubject("Exception thrown in GeneDataTools.getBQTLs");
                 myAdminEmail.setContent("There was an error while running getBQTLs.",er);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                }
            }
        //}
        
        return bqtl;
    }
    public BQTL getBQTL(String id,String genomeVer){
        
        BQTL bqtl=null;
        session.removeAttribute("getBQTLsERROR");
        boolean run=true;
        
            String query="select pq.*,c.name from public_qtls pq, chromosomes c "+
                            "where pq.genome_id='"+genomeVer+"' and pq.rgd_id="+id+
                            "and pq.chromosome=c.chromosome_id";
            Connection conn=null;
            try{ 
            try{
                log.debug("SQL bQTL FROM QUERY\n"+query);
                conn=pool.getConnection();
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                if(rs.next()){
                    String mgiID=rs.getString(2);
                    String rgdID=rs.getString(3);
                    String symbol=rs.getString(5);
                    String name=rs.getString(6);
                    double lod=rs.getDouble(8);
                    double pvalue=rs.getDouble(9);
                    String trait=rs.getString(10);
                    String subTrait=rs.getString(11);
                    String traitMethod=rs.getString(12);
                    String phenotype=rs.getString(13);
                    String diseases=rs.getString(14);
                    String rgdRef=rs.getString(15);
                    String pubmedRef=rs.getString(16);
                    String relQTLs=rs.getString(18);
                    String candidGene=rs.getString(17);
                    long start=rs.getLong(19);
                    long stop=rs.getLong(20);
                    String mapMethod=rs.getString(21);
                    String chromosome=rs.getString(23);
                    bqtl=new BQTL(id,mgiID,rgdID,symbol,name,trait,subTrait,traitMethod,phenotype,diseases,rgdRef,pubmedRef,mapMethod,relQTLs,candidGene,lod,pvalue,start,stop,chromosome);
                }
                ps.close();
                conn.close();
            }catch(SQLException e){
                log.error("Error retreiving bQTLs.",e);
                e.printStackTrace(System.err);
                session.setAttribute("getBQTLsERROR","Error retreiving region bQTLs.  Please try again later.  The administrator has been notified of the problem.");
                 Email myAdminEmail = new Email();
                 myAdminEmail.setSubject("Exception thrown in GeneDataTools.getBQTLs");
                 myAdminEmail.setContent("There was an error while running getBQTLs.",e);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                }
            }finally{
                try {
                    if(conn!=null)
                        conn.close();
                } catch (SQLException ex) {
                }
            }
            }catch(Exception er){
                er.printStackTrace(System.err);
                session.setAttribute("getBQTLsERROR","Error retreiving region bQTLs.  Please try again later.  The administrator has been notified of the problem.");
                 Email myAdminEmail = new Email();
                 myAdminEmail.setSubject("Exception thrown in GeneDataTools.getBQTLs");
                 myAdminEmail.setContent("There was an error while running getBQTLs.",er);
                try {
                    myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
                } catch (Exception mailException) {
                    log.error("error sending message", mailException);
                }
            }
        
        
        return bqtl;
    }
    
    public String getBQTLRegionFromSymbol(String qtlSymbol,String organism,String genomeVer){
        return this.getBQTLRegionFromSymbol(qtlSymbol,organism,genomeVer, pool);
    }
    
    public String getBQTLRegionFromSymbol(String qtlSymbol,String organism,String genomeVer,DataSource pool){
        if(qtlSymbol.startsWith("bQTL:")){
            qtlSymbol=qtlSymbol.substring(5);
        }
        String region="";
        String query="select pq.*,c.name from public_qtls pq, chromosomes c "+
                        "where pq.genome_id='"+genomeVer+"' "+
                        "and pq.chromosome=c.chromosome_id "+
                        "and pq.QTL_SYMBOL='"+qtlSymbol+"'";
        
        try{ 
        try(Connection conn=pool.getConnection()){
            //log.debug("SQL eQTL FROM QUERY\n"+query);
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                long start=rs.getLong(19);
                long stop=rs.getLong(20);
                String chromosome=rs.getString(23);
                region="chr"+chromosome+":"+start+"-"+stop;
            }
            ps.close();
            
        }catch(SQLException e){
            log.error("Error retreiving bQTL region from symbol.",e);
            e.printStackTrace(System.err);
            session.setAttribute("getBQTLRegionFromSymbol","Error retreiving bQTL region from symbol.  Please try again later.  The administrator has been notified of the problem.");
             Email myAdminEmail = new Email();
             myAdminEmail.setSubject("Exception thrown in GeneDataTools.getBQTLs");
             myAdminEmail.setContent("There was an error while running getBQTLs.",e);
            try {
                myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
            } catch (Exception mailException) {
                log.error("error sending message", mailException);
            }
        }
        }catch(Exception er){
            er.printStackTrace(System.err);
            session.setAttribute("getBQTLsERROR","Error retreiving bQTL region from symbol.  Please try again later.  The administrator has been notified of the problem.");
             Email myAdminEmail = new Email();
             myAdminEmail.setSubject("Exception thrown in GeneDataTools.getBQTLs");
             myAdminEmail.setContent("There was an error while running getBQTLs.",er);
            try {
                myAdminEmail.sendEmailToAdministrator((String) session.getAttribute("adminEmail"));
            } catch (Exception mailException) {
                log.error("error sending message", mailException);
            }
        }
        
        return region;
    }
        
    public void addQTLS(ArrayList<Gene> genes, ArrayList<EQTL> eqtls){
        HashMap eqtlInd=new HashMap();
        for(int i=0;i<eqtls.size();i++){
            EQTL tmp=eqtls.get(i);
            eqtlInd.put(tmp.getProbeSetID(), i);
        }
        for(int i=0;i<genes.size();i++){
            genes.get(i).addEQTLs(eqtls,eqtlInd,log);
        }
    }
    
    public void addFromQTLS(ArrayList<Gene> genes, HashMap transcriptClustersCore,HashMap transcriptClustersExt,HashMap transcriptClustersFull){
        for(int i=0;i<genes.size();i++){
            if(genes.get(i).getGeneID().startsWith("ENS")){
                genes.get(i).addTranscriptCluster(transcriptClustersCore,transcriptClustersExt,transcriptClustersFull,log);
            }
        }
    }

    public String getGenURL() {
        return returnGenURL;
    }

    public String getUCSCURL() {
        return returnUCSCURL;
    }

    

    public String getOutputDir() {
        return returnOutputDir;
    }

    public String getGeneSymbol() {
        return returnGeneSymbol;
    }
    
    
    
}
class RegionDirFilter implements FileFilter{
    String toCheck="";
    
    RegionDirFilter(String toCheck){
        this.toCheck=toCheck;
    }
    
    public boolean accept(File file) {
        boolean ret=true;
        if(!file.isDirectory()){
            ret=false;
        }
        if(!file.getName().startsWith(toCheck)){
            ret=false;
        }
        return ret;
    }
    
}