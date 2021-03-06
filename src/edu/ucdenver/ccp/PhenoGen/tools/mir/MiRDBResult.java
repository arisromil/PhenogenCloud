package edu.ucdenver.ccp.PhenoGen.tools.mir;

/*
*   Spencer Mahaffey
*   March 2014
*
*   Provide functions to run multiMiR and link results to Genes and eQTLs
*   also to create interactive circos plots for results.
*
*/


import edu.ucdenver.ccp.util.FileHandler;
import edu.ucdenver.ccp.util.ObjectHandler;


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
import javax.sql.DataSource;
import java.lang.Thread;



public class MiRDBResult{
    private String database="";
    private String accession="";
    private String id="";
    private String experiment="";
    private String support="";
    private String pubmedID="";
    private double score=0.0;
    private String link="";
    private String entrez="";
    private String ensembl="";
    private String geneSymbol="";
    
    public MiRDBResult(){
        
    }
    
    public MiRDBResult(String db,String accession,String id,double score,String link,String entrez,String ensembl,String geneSym){
        this.database=db;
        this.accession=accession;
        this.id=id;
        this.score=score;
        this.link=link;
        this.entrez=entrez;
        this.ensembl=ensembl;
        this.geneSymbol=geneSym;
    }

    public MiRDBResult(String db,String accession,String id,String experiment,String support, String pubmedID,String link,String entrez,String ensembl,String geneSym){
        this.database=db;
        this.accession=accession;
        this.id=id;
        this.experiment=experiment;
        this.support=support;
        this.pubmedID=pubmedID;
        this.link=link;
        this.entrez=entrez;
        this.ensembl=ensembl;
        this.geneSymbol=geneSym;
    }
    
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public String getPubmedID() {
        return pubmedID;
    }

    public void setPubmedID(String pubmedID) {
        this.pubmedID = pubmedID;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getEntrez() {
        return entrez;
    }

    public void setEntrez(String entrez) {
        this.entrez = entrez;
    }

    public String getEnsembl() {
        return ensembl;
    }

    public void setEnsembl(String ensembl) {
        this.ensembl = ensembl;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }
    
    
    
}