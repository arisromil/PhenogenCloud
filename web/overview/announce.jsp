<%@ include file="/web/common/headerOverview.jsp" %>
<%--
 *  Author: Spencer Mahaffey
 *  Created: May, 2013
 *  Description:  
 *
 *  Todo: 
 *  Modification Log:
 *      
--%>

    
		<H2>Announcements</H2>
                    <div  style="overflow:auto;height:92%;">
                        <H2 style="font-weight: bold;font-size:20px;">Phenogen in the Cloud</H2>
                        <div style="margin-left:5px;">
                            This is the new home for PhenoGen running in the cloud.  As such, a large number of changes have occurred in the background that will help us maintain the site and improve performance.  Please excuse some ongoing bugs as we are beta testing the site now.  This is not currently the recommended version.
                            However, we would appreciate any help testing the supported features by simply using this site as you would the previous site.
                        </div>
                        <H2> REST API Survey 2/20/2018</H2>
                        <div style="margin-left:5px;">
                            Please help us prioritize effort on a REST API to provide access to data on Phenogen.  Take the 1 question survey <a href="#survey">below</a>.
                        </div>
                        <H2>PhenoGen v3.4.2 3/9/2018</H2>
                        <div style="margin-left:5px;">
                            Added read depth count tracks to the genome browser for whole brain and liver for the inbred strains:
                            ACI, Dark-Agouti, Cop, F344-NCl, F344-NHsd, LEW-Crl, LEW-SsNHsd, SHRSP, SR-JrHsd, SS-JrHsd, and WKY.
                        </div>
                        <H2>PhenoGen v3.4.1 2/16/2018</H2>
                        <div style="margin-left:5px;">
                            The Genome/Transcriptome Data Browser can now look up genes by either their gene or transcript PhenoGen ID.
                        </div>
                        <H2>NIDA Genetics Consortium Meeting Poster</H2>
                        <div style="margin-left:5px;">
                            <span style=" font-weight: bold;"><a href="<%=webDir%>overview/NIDA_Jan_2018.pdf">NIDA Meeting Poster</a> - </span> 
                            Download the poster from the NIDA meeting with an outline of ways to use our WGCNA Modules and recent examples of our use with phenotype QTLs.
                            Download <a href="<%=webDir%>overview/NIDA_Jan_2018.pdf">here</a>.
                        </div>
                        <H2>PhenoGen v3.4 12/10/2017</H2>
                        <div style="margin-left:5px;">
                            <span style=" font-weight: bold;">Recombinant Inbred Small RNA - </span> Added expression data for small RNA features across RI Panel in Whole Brain and Liver. 
                        </div>
                        <H2>PhenoGen v3.3 4/30/2017</H2>
                        <div style="margin-left:5px;">
                            <span style=" font-weight: bold;">Recombinant Inbred Total RNA - </span> Added expression data for reconstructed transcripts across RI Panel in Whole Brain and Liver.  Added RNA-Seq based WGCNA for Whole Brain and Liver.  
                            
                        </div>
                        <H2>PhenoGen v3.2 11/13/2016</H2>
                        <div style="margin-left:5px;">
                            <span style=" font-weight: bold;">Small RNA - </span> added tracks and detail on all known and novel (predicted by MiRDeep and SNOSeeker) small RNAs in Brain, Heart, and Liver from the BNLx/SHR parental strains.
                            <BR>
                            <span style=" font-weight: bold;">Merged Total RNA Transcriptome - </span> added a track with the merged transcriptome from the 3 available tissues and assigned new unique PhenoGen IDs to all novel transcripts.
                        </div>
                        <H2>PhenoGen v3.1 6/15/2016</H2>
                        <div style="margin-left:5px;">
                            <span style=" font-weight: bold;">Rn6 - </span> is available in the browser, for gene list analysis, and both RNA-Seq datasets and microarray datasets have been updated.
                            <BR>
                            <span style=" font-weight: bold;">Rat WGCNA - </span> Heart and Liver have been added to the rn6 data.
                        </div>
                         <H2>PhenoGen v3.0 5/31/2016</H2>
                        <div style="margin-left:5px;">
                            <span style=" font-weight: bold;">Anonymous Gene List</span> - You can now use our gene list analysis tools without registering.  We encourage you to link your email so you don't loose access to previous work.<BR>
                            <span style=" font-weight: bold;">Rn6 (June 2016):</span> Rn6 - Will be available in June 2016.  Rn6 and Rn5 will be available in the genome browser, public HXB datasets, and gene list analysis tools.
                              
                        </div>
                        <H2>PhenoGen v2.16.1 11/10/2015</H2>
                        <div style="margin-left:5px;">
                            <span style=" font-weight: bold;">Security Updates</span> - We now require using HTTPS so all of the data transmitted between your browser and our server is encrypted.  Update your bookmarks.<BR>
                            <span style=" font-weight: bold;">Future Update:</span> Rn6 - We are still working on updating the Microarrays and RNA-Seq data to Rn6.  Our next major update will include Rn6.
                              
                        </div>
                        <H2>PhenoGen v2.16 7/21/2015</H2>
                        <div style="margin-left:5px;">
                            <ul>
                                <li>Gene List tabs have been reformatted so you can submit submit new analyses and view results and running analyses status from a single page.</LI> 
                                <li>GO term summaries are available for Gene Lists.</li>
                                <li>MuliMiR results are available for Rat both in Gene Lists and the browser(individual genes, WGCNA modules).</li>
                            </ul>   
                              
                        </div>
                        <H2>Minor Updates 6/8/2015</H2>
                        <div style="margin-left:5px;">
                            We've made a couple of minor updates since the last release. 
                            <ul>
                                <li>The UCSC Repeat Mask track is now available.</li>
                                <li>You now have the ability to add tracks without going into the edit view option.</li>
                                <li>We've also fixed a number of bugs and made general functionality improvements.</li>
                            </ul>   
                              
                        </div>
                        <H2>Workshop Video/Slides 4/16/2015</H2>
                        <div style="margin-left:5px;text-align: center;">
                            Watch the workshop:<BR><BR>
                            <video id="demoVideo" width="250px"  controls="controls" poster="<%=webDir%>demo/slides2_350.png" preload="none">
                                    	<source src="<%=webDir%>demo/workshop.mp4" type="video/mp4">
                                        <source src="<%=webDir%>demo/workshop.webm" type="video/webm">
                                          <object data="<%=webDir%>demo/workshop.mp4" width="100%" >
                                          </object>
                                          Your browser is not likely to work with the Genome Browser if you are seeing this message.  Please see <a href="<%=commonDir%>siteRequirements.jsp">Browser Support/Site Requirements</a>
                                    </video>
                            <!--<a href="<%=webDir%>overview/PhenoGen.workshop.16Apr15.pdf"><img src="<%=webDir%>overview/slides_150.png" /></a>--><BR>
                                          OR<BR>
                            Download the slides from the Informatics Workshop <a href="<%=webDir%>overview/PhenoGen.workshop.16Apr15.pdf">here</A>.
                        </div>
                        <H2>v2.15 of PhenoGen 3/7/2015</H2>
                        <div style="margin-left:5px;">
                                <img src="<%=webDir%>overview/browseWGCNA_mir_150.png" /><img src="<%=webDir%>overview/browseWGCNA_go_150.png" /><BR />
                        	We've added GO term summary and miRNA targeting views to the Weighted Gene Co-expression Network Analysis.  Look at what's new for a summary of changes.
                        </div>
                        <H2>HTTPS support 2/9/2015</H2>
                        <div style="margin-left:5px;">
                        <%if(request.getServerPort()==80){%>
                        
                            We now support https to keep your connections more secure.  We will eventually redirect all traffic to the secure site, 
                            but for now feel free to try it out here: <a href="https://phenogen.ucdenver.edu/PhenoGen/"> https://phenogen.ucdenver.edu/PhenoGen/</a>
                        
                        <%}else{%>
                            
                            Thank you for trying the secure site.  You can always switch back to the regular site: <a href="http://phenogen.ucdenver.edu/PhenoGen/"> http://phenogen.ucdenver.edu/PhenoGen/</a>
                        
                        <%}%>
                        </div>
                        <H2>v2.14 of PhenoGen 1/10/2015</H2>
                        <div style="margin-left:5px;">
                                <img src="<%=webDir%>overview/browseWGCNA_150.png" /><img src="<%=webDir%>overview/browseWGCNA_eQTL_150.png"/><BR />
                        	We've added Weighted Gene Co-expression Network Analysis.  Look at what's new for a summary of changes.
                        </div>
                    	<H2>v2.13 of PhenoGen 9/27/2014</H2>
                        <div style="margin-left:5px;">
                        	We've updated PhenoGen.  Look at what's new for a summary of changes.
                        </div>
                    	<H2>Added multiMiR</H2>
                        <div style=" margin-left:5px;">
                        	<img src="<%=imagesDir%>multimir_300.png"/><BR />
                        	Using multiMiR(an R package available <a href="http://multimir.ucdenver.edu/" target="_blank">here</a>) you can view validated and predicted miRNAs that target specific genes.  You can also select a miRNA and view all genes targeted by the miRNA.  multiMiR is avaialble as a new tab for a selected gene in the Genome/Transcriptome Data Browser and in Gene Lists after selecting a list. It is currently available only for mouse genes, but will be available in rat soon.
                        </div>
                        <H2>Added Rat Liver Transcriptome</H2>
                        <div style=" margin-left:5px;">
                        	We've added rat liver tracks including, a transcriptome reconstructiong track, splice junction track, and stranded read depth count tracks.  Available in the Genome/Transcriptome Browser.
                        </div>
                    	<H2>Follow on Facebook/Google+/Twitter</H2>
                        
                        <div style=" margin-left:5px;">
                        	Follow PhenoGen to keep up with new features, demonstrations, and help by providing feedback to direct future updates.<BR />
                           	<div style="float:left;display:inline-block;position:relative;top:0px;padding-right:5px;">  	
                                    <div class="fb-follow" data-href="https://www.facebook.com/phenogen" data-width="50px" data-height="16px" data-colorscheme="dark" data-layout="button" data-show-faces="true"></div>
                           	</div>
                                <div style="float:left;display:inline-block;">

                                    <a href="https://twitter.com/phenogen" class="twitter-follow-button" data-show-count="false" data-show-screen-name="false" data-lang="en" style="margin-top:5px;"></a>
                                </div>
                           
                            <BR /><BR />
                        </div>
                    	<H2>RNA-Seq Data Summary Graphics</H2>
                        <div style=" margin-left:5px;">
                    	Rat Brain RNA-Seq data summary graphics are now available. Click below to browse the RNA-Seq data summary:<BR />
                        <div style="text-align:center;">
                        <ul>
                        <li><a href="web/graphics/genome.jsp">View Genome Coverage</a></li>
                        <li><a href="web/graphics/transcriptome.jsp">View Reconstructed Long RNA Genes(Rat Brain Transcriptome)</a></li>
                        </ul>
                        <a href="web/graphics/genome.jsp"><img src="<%=imagesDir%>rnaseq_genome_100.gif" /></a>
                        <a href="web/graphics/transcriptome.jsp"><img src="<%=imagesDir%>rnaseq_transcriptome_100.gif" /></a>
                        </div><BR />
                        Reconstructed transcripts from this RNA-Seq data are still combined with PhenoGen array data in <a href="<%=commonDir%>selectMenu.jsp?menuURL=<%=accessDir%>createAnnonymousSession.jsp?url=<%=contextRoot%>gene.jsp">Genome/Transcriptome Data Browser</a>.
                        </div>
                        
                   </div>
                                       

                    <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src="//platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>

<%@ include file="/web/overview/ovrvw_js.jsp" %>