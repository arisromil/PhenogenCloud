########################## multipleTest.R ################
#
# This function runs the specified test on the data
#
# Parms:
#    OutputFile     = name of output file containing input and added test info, 
#    GeneNumberFile = # of genes (output from Affy.statistics)
#    InputFile      = input file containing Absdata, Avgdata, 
#                         Gnames, grouping, groups, Snames, p, Procedure
#                         (OutputFile from Affy.statistics)	
#    mt.method      =  see details below
#    MCC.parameter1 =     "
#    MCC.parameter2 =     "
#    MCC.parameter3 =     "
#    MCC.parameter4 =     "
#	 error.file		=	file with error details for storey method
#
# mt.method
# -- NoTest
# ----- MCC.parameter1 == 0.05 (cutoff threshold)
# -- BioC MCC options:
# --- Bonferroni
# --- Holm
# --- Hochberg
# --- SidakSS
# --- SidakSD
# --- BH
# --- BY
# ----- MCC.parameter1 == 0.05 (cutoff threshold)
# -- BioC Permutation
# --- MaxT
# --- MinP
# ----- MCC.parameter1 == 0.05 (cutoff threshold)
# ----- MCC.parameter2 == t ('test' for maxT and minP:
#					t, t.equalvar, wilcoxon, f, pairt, blockf)
# ----- MCC.parameter3 == 10000 ('B' for maxT and minP:
#					10,000  == ten thousand permutation
#					0	== maximum permutation)
# ----- MCC.parameter4 == y ('nonpara' for maxT and minP: y or n)
# -- Storey's pFDR
# ----- MCC.parameter1 == 0.05 (cutoff threshold wrt q-value)
# ----- MCC.parameter2 == method to estimate tuning parameter ("smoother" or "bootstrap")
#
# Returns:
#    nothing
#
# Writes out:
#    .rdata file (OutputFile) containing Absdata, Avgdata, 
#            Gnames, grouping, groups, Snames, p, Procedure, 
#            GenebankID, kw.p, ttest.p, adjp
#    text file (GeneNumberFile) containing gene count
#
# Sample Call:
#    multipleTest(InputFile =  'HapLap.statistics.output.Rdata', mt.method = 'FDR', MCC.parameter1 = seq(0.001, 0.9, 0.001), MCC.parameter2 = -99, MCC.parameter3 = 'null', MCC.parameter4 = 'null', OutputFile = 'HapLap.multipleTest.output.Rdata', GeneNumberFile = 'HapLap.GeneNumberCount.txt', error.file="errorFile.txt", Run.At.Home = F)
#
#
#  Function History
#     ?????????   Tzu Phang   Created      
#     3/21/2005   Diane Birks  Modified: added logging via writelog,
#                                fixed bug for maxT,minP pairt and blockf options:
#                                classlabels must be 0,1,0,1,... not 0,0,...1,1...
#                                temp.data must be same way too.   
#	05/01/06	Laura Saba	Modified: added group means and pvalues (both raw and adjusted to output)
#   05/31/06	Laura Saba 	Modified: added Storey version of FDR
#	12/04/06	Laura Saba	Modified: Consolidated programs for CodeLink and Affymetrix
#	10/01/07	Laura Saba	Modified: update minP and maxT to handle situations when all values are equal for one probe
#	04/09/09	Laura Saba	Modified: removed logic to turn MCC.paramter2 into a numeric variable if not calling Storey method or minP maxT
#	07/20/10	Laura Saba	Modified: added error.file parameter and error message for Storey adjustment
#	09/20/10	Laura Saba	Modified: removed loading of wystepdown.R
#
####################################################


multipleTest <- function(InputFile, mt.method, MCC.parameter1 = -99, MCC.parameter2= -99, MCC.parameter3 = -99, MCC.parameter4 = -99, OutputFile, GeneNumberFile, error.file,Run.At.Home = F){


  ###################################################
  ###################################################
  ###				          ###################
  ### 	START OF PROGRAM	    ###################
  ###				          ###################
  ###################################################
  ###################################################

  ################# Housekeeping ############################

  # set up libraries and R functions needed
  library(multtest)
  library(qvalue)
  fileLoader('fdr.R')


  #################################################
  ## process data	
  ##						
	
  load(InputFile)

  ###  No multiple testing correction applied  ###
	if(!Run.At.Home) logflag=G_WriteLogAffyMultTest

  if(mt.method == 'NoTest'){
	if(!Run.At.Home){
		writelog(flag=logflag,'No multiple Comparison Tesing ....')
	}else{
		cat('No multiple Comparison Tesing ....\n\n')
	}
	adjp = p
	gene.index = p <= MCC.parameter1
	gene.index = as.logical(gene.index)
	if(!Run.At.Home){
		writelog(flag=logflag,'gene.index has', sum(gene.index))
	}else{
		cat('gene.index has', sum(gene.index),'\n\n')
	}
  }

  ###  Bonferroni, Holm, Hochberg, Sidak, and FDR  ###

  else if(mt.method == 'Bonferroni' | mt.method == 'Holm' | mt.method == 'Hochberg' | mt.method == 'SidakSS' | mt.method == 'SidakSD' | mt.method == 'BH' | mt.method == 'BY'){

	if(!Run.At.Home){
		writelog(flag=logflag,'Running BioC using mt.method', mt.method, ' MCC method ....')
	}else{
		cat('Running BioC using mt.method', mt.method, ' MCC method ....\n\n')
	}
     
     	if(length(Gnames)>1){
		res = mt.rawp2adjp(p, proc = mt.method)
		adjp = res$adjp[order(res$index),]  # reset to original order
		adjp = adjp[,2]
	}

	else if(length(Gnames)==1) {
		adjp = p
	}

	gene.index = adjp <= MCC.parameter1
	gene.index = as.logical(gene.index)

	if(!Run.At.Home){
		writelog(flag=logflag,'adjp has', length(adjp))
		writelog(flag=logflag,'gene.index has', sum(gene.index))
	}else{
		cat('adjp has', length(adjp),'\n\n')
		cat('gene.index has', sum(gene.index),'\n\n')
	}

  }
  else if(mt.method == 'maxT' | mt.method == 'minP'){

	if(length(Gnames)>1){
		if(max(grouping)>2){
			if(!Run.At.Home){
				writelog(flag=logflag,user=TRUE,'maxT and minP are only designed for two phenotype design experiment!')
			}else{
				cat('maxT and minP are only designed for two phenotype design experiment!\n\n')
			}
			gene.index = c()
		}

		else{
			if(!Run.At.Home){
				writelog(flag=logflag,'Running BioC ', mt.method, ' permutation MCC method ....')
			}else{
				cat('Running BioC ', mt.method, ' permutation MCC method ....\n\n')
			}

			data = Avgdata
			temp.data = c()
			grouping = c()
                  for(i in 1:length(groups)){
			      temp.data = cbind(temp.data, data[,groups[[i]]])
			      grouping = c(grouping, rep(i, length(groups[[i]])))
			}
                                              
			if(!Run.At.Home){
				writelog(flag=logflag,'grouping is ', grouping)
			}else{
				cat('grouping is ', grouping,'\n\n')
			}
	
			classlabels = grouping - 1
	
			if(!Run.At.Home){
				writelog(flag=logflag,'classlabels', classlabels)
				writelog(flag=logflag,'temp.data is ', dim(temp.data)[2])
			}else{
				cat('classlabels', classlabels,'\n\n')
				cat('temp.data is ', dim(temp.data)[2],'\n\n')
			}

			if(mt.method == 'maxT') {
				resT = mt.maxT(temp.data, classlabel = classlabels, test = MCC.parameter2, B = MCC.parameter3, nonpara = MCC.parameter4)
			}else{
				resT = mt.minP(temp.data, classlabel = classlabels, test = MCC.parameter2, B = MCC.parameter3, nonpara = MCC.parameter4)
			}
	
			ord = order(resT$index) # original order
			rawp = resT$rawp[ord]
			adjp = resT$adjp[ord]

			adjp <- replace(adjp,is.na(adjp),1)

			gene.index = adjp <= MCC.parameter1
			gene.index = as.logical(gene.index)

			if(!Run.At.Home){
				writelog(flag=logflag,'adjp has', length(adjp))
				writelog(flag=logflag,'gene.index has', sum(gene.index))
			}else{
				cat('adjp has', length(adjp),'\n\n')
				cat('gene.index has', sum(gene.index),'\n\n')
			}
		}
	}
  	else{
		adjp=p
		gene.index = adjp <= MCC.parameter1
		gene.index = as.logical(gene.index)
	}
  }

  else if(mt.method == 'Storey'){

	if(!Run.At.Home){
		writelog(flag=logflag,'Running Storey FDR method....')
	}else{
		cat('Running Storey FDR method ....\n\n')
	}

	qobj = qvalue(p, pi0.meth = MCC.parameter2)
	
	if(!is.list(qobj) & length(Gnames)!=1){
		write("PhenoGen was not able to apply the Storey method for multiple testing correction on your gene list. For these values to be calculated, the algorithm must first estimate the proportion of genes that are not differentially expressed.  PhenoGen was not able to calculate this value based on standard model inputs. An extremely large portion of genes being differentially expressed most often causes this.  Rather than manually tuning parameter, we suggest using a different method for estimating a false discovery rate, such as Benjamini and Hochberg or Benjamini and Yekutieli.",file=error.file)
		stop("See txt output")
		}
	
	if(length(Gnames)>1) adjp = qobj$qvalue
	else if(length(Gnames)==1) adjp = p

	gene.index = adjp <= MCC.parameter1
	gene.index = as.logical(gene.index)

	if(!Run.At.Home){
		writelog(flag=logflag,'adjp has', length(adjp))
		writelog(flag=logflag,'gene.index has', sum(gene.index))
	}else{
		cat('adjp has', length(adjp),'\n\n')
		cat('gene.index has', sum(gene.index),'\n\n')
	}

  }
  else{
	if(!Run.At.Home){
		writelog(flag=logflag,user=TRUE,'The multiple correction testing you have suggested is not presently implemented, please try again!')
	}else{
		cat('The multiple correction testing you have suggested is not presently implemented, please try again!\n\n')
	}
  }
		

  #################################
  ##                             ##
  ##        REMOVING GENES       ##
  ##                             ##
  #################################
	
  if(exists('gene.index') && sum(gene.index) != 0){
	if(length(Gnames) > 1){
		if(is.logical(gene.index)){
			Avgdata = Avgdata[gene.index,]
			Gnames = Gnames[gene.index]
			Absdata = Absdata[gene.index,]
			p = p[gene.index]
			adjp = adjp[gene.index]
			stats = stats[,gene.index]
		}
		else{
			Avgdata = Avgdata[-gene.index,]
			Gnames = Gnames[-gene.index]
			Absdata = Absdata[-gene.index,]
			p = p[-gene.index]
			adjp = adjp[-gene.index]
			stats = stats[,-gene.index]
		}
	}
  }
  else{
		Avgdata = c()
		Gnames = c()
		Absdata = c()
		p = c()
  }



		
	temp = c()

	if(length(MCC.parameter1) > 1){
		for(i in 1:length(MCC.parameter1)){
			temp = paste(temp, MCC.parameter1[i], sep = ' ')
		}
		MCC.parameter1 = temp
	}


	if(!Run.At.Home){
		writelog(flag=logflag,'parameter1 is ', MCC.parameter1)
	}else{

		if(mt.method == 'FDR'){
			cat('parameter1 is ', length(MCC.parameter1),'\n\n')
		}else{
			cat('parameter1 is ', MCC.parameter1,'\n\n')
		}
	}

	temp = c()

	if(length(MCC.parameter2) > 1){
		for(i in 1:length(MCC.parameter2)){
			temp = paste(temp, MCC.parameter2[i], sep = ' ')
		}
		MCC.parameter2 = temp
	}

	if(!Run.At.Home){
		writelog(flag=logflag,'parameter2 is ', MCC.parameter2)
	}else{
		cat('parameter2 is ', MCC.parameter2,'\n\n')
	}


	temp = c()

	if(length(MCC.parameter3) > 1){
		for(i in 1:length(MCC.parameter3)){
			temp = paste(temp, MCC.parameter3[i], sep = ' ')
		}
		MCC.parameter3 = temp
	}

	if(!Run.At.Home){
		writelog(flag=logflag,'parameter3 is ', MCC.parameter3)
	}else{
		cat('parameter3 is ', MCC.parameter3,'\n\n')
	}


	temp = c()

	if(length(MCC.parameter4) > 1){
		for(i in 1:length(MCC.parameter4)){
			temp = paste(temp, MCC.parameter4[i], sep = ' ')
		}
		MCC.parameter4 = temp
	}

	if(!Run.At.Home){
		writelog(flag=logflag,'parameter4 is ', MCC.parameter4)
	}else{
		cat('parameter4 is ', MCC.parameter4,'\n\n')
	}


			

	Procedure <- paste(Procedure, 'Function=multipleTest.R;','mt.method=',mt.method,';','MCC.method=',mt.method,';','MCC.parameter1=',MCC.parameter1,';','MCC.parameter2=',MCC.parameter2,'|',sep = '')
	
######################################
##  Eliminating Duplicate Genes (Needed for CodeLink Data Only)
###################################### 

if (sum(gene.index)>1){

	Avgdata = Avgdata[!duplicated(Gnames),]
	Absdata = Absdata[!duplicated(Gnames),]
	p = p[!duplicated(Gnames)]
	adjp = adjp[!duplicated(Gnames)]
	stats = stats[,!duplicated(Gnames)]

      Gnames<-Gnames[!duplicated(Gnames)]
}

      cat('Gene numbers is ', length(Gnames), '\n')

	cat(file = GeneNumberFile, length(Gnames))

	save(Absdata, Avgdata, Gnames, grouping, groups, Snames, stats, p, Procedure, adjp,  file = OutputFile)


} #END 
