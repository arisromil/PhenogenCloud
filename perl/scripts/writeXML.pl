#!/usr/bin/perl

use Bio::EnsEMBL::Registry;
use XML::LibXML;
use XML::Simple;

#use strict;

require 'ReadAffyProbesetDataFromDB.pl';
#require 'createPng.pl';
#require 'addAlternateID.pl';
#require 'convertBedToBigBed.pl';

sub getFeatureInfo
{
	# Routine to get 
    my $feature = shift;

    my $stable_id  = $feature->stable_id();
    my $seq_region = $feature->slice->seq_region_name();
    my $start      = $feature->seq_region_start();
    my $stop        = $feature->seq_region_end();
    my $strand     = $feature->seq_region_strand();

    return ($stable_id, $seq_region, $start, $stop, $strand );
}

sub find
{
    
    my $lookForGene = shift;
    my $list=shift;
    my $ret=0;
    print "Find: $lookForGene\n";
    foreach(my $testName, @$list){
	print "$$testName:$lookForGene ";
	if($$testName eq $lookForGene){
	    print "Found";
	    $ret=1;
	}
	print "\n";
    }

    return $ret;
}

sub createXMLFile
{
	#This subroutine reads data from two sources
	#It reads data from ensembl using their perl API
	#It reads data from Affy via downloaded files
	#
	#Inputs:
	# 	Name with path of UCSC bed file.  This file must be in the directory /data/ucsc on Phenogen, or must be moved there.
	#   Name with path of png output file
	#   Name with path of xml output file
	#	Species for example, Rat
	#	Type: for example, 'Core'
	#	The ensembl Gene Names for example 'ENSRNOG00000001285' or 'ENSRNOG00000001285,ENSRNOG00000001286,ENSRNOG00000001287'
	#
	#
	#
	#

	# Read in the arguments for the subroutine	
	my($xmlOutputFileName,$species,$type,$geneNames,$userName,$dataSetID,$arrayTypeID,$genomeVer,$dsn,$usr,$passwd,$ensHost,$ensPort,$ensUsr,$ensPasswd)=@_;
	
	my @geneNamesList=split(/,/,$geneNames);
	
	
	#
	# Zero a bunch of counters
	#
	my $cntTranscripts=0;
	my $cntProbesets=0;
	my $cntExons=0;
	my $cntGenes=0;
	my $cntMatchingProbesets=0;
	my $sliceStart;

	my %GeneHOH; # This is the big data structure to hold information about genes, transcripts, exons, probesets
	my $GeneHOHRef;


	my $registry = 'Bio::EnsEMBL::Registry';
	my $dbAdaptorNum=-1;
	my $ranEast=0;
	eval{
	    print "trying local\n";
	    $dbAdaptorNum =$registry->load_registry_from_db(
		-host => $ensHost, #'ensembldb.ensembl.org', # alternatively 'useastdb.ensembl.org'
		-port => $ensPort,
		-user => $ensUsr,
		-pass => $ensPasswd
	    );
	    print "local finished:$dbAdaptorNum\n";
	    1;
	}or do{
	    print "local ensembl DB is unavailable\n";
	    $dbAdaptorNum=-1;
	};
	if($dbAdaptorNum==-1){
	    print "trying useastdb\n";
	    $ranEast=1;
	    eval{
		    $dbAdaptorNum=$registry->load_registry_from_db(
			-host => 'useastdb.ensembl.org', #'ensembldb.ensembl.org', # alternatively 'useastdb.ensembl.org'
			-port => 5306,
			-user => 'anonymous'
		    );
		    print "east mirror finished:$dbAdaptorNum\n";
		    1;
	    }or do{
		print "ensembl east DB is unavailable\n";
		$dbAdaptorNum=-1;
	    };
	}
	if($ranEast==1 && $dbAdaptorNum<1){
	    print "trying ensembldb\n";
	    # Enable this option if problems occur connecting the above option is faster, but only has current and previous versions of data
	    $dbAdaptorNum=$registry->load_registry_from_db(
		-host => 'ensembldb.ensembl.org', 
		-user => 'anonymous'
	    );
	    print "main finished:$dbAdaptorNum\n";
	}
	
	

	print "connected\n";

	#print "connected\n";
	my $slice_adaptor = $registry->get_adaptor( $species, $type, 'Slice' );
	
	my @genelist=();
	my @slicelist=();
	#print "gene list size:".@geneNamesList."\n";
	#my $geneName = shift @geneNamesList;
	while ( my $geneName1 = shift @geneNamesList ) {
	    #print "Get:$geneName1\n";
	    my $tmpslice = $slice_adaptor->fetch_by_gene_stable_id( $geneName1, 50 ); # the 50 just returns a little more on the chromosome. shortened from 5000 since this returns too much.
	    # Get all the genes.  Theoretically there should only be one, but possibly there might be more????
	    my $genes = $tmpslice->get_all_Genes();
	    while(my $tmpgene=shift @{$genes}){
		push(@genelist, $tmpgene);
		push(@slicelist, $tmpslice);
	    }
	    #print "gene size found:".@{$genes}."\n";
	    #print "gene list:".@genelist."\n";
	}

	my @addedGeneList=();
	# Loop through Genes
	while ( my $gene = shift @genelist ) {
		my $slice=shift @slicelist;
		my ($geneName, $geneRegion, $geneStart, $geneStop,$geneStrand) = getFeatureInfo($gene);
		my $geneChrom = "chr$geneRegion";
		my $geneBioType = $gene->biotype();
		my $geneExternalName = $gene->external_name();
		my $found=0;
		print "Find: $geneName\n";
		foreach $testName (@addedGeneList){
		    print "$testName:$geneName ";
		    if($testName eq $geneName){
			print "Found";
			$found=1;
		    }else{
			print "Not found";
		    }
		    print "\n";
		}

	    if(length($geneRegion)<3&&$found==0){
		push(@addedGeneList,$geneName);
		$GeneHOH{Gene}[$cntGenes] = {
			start => $geneStart,
			stop => $geneStop,
			ID => $geneName,
			strand=>$geneStrand,
			chromosome=>$geneChrom,
			biotype => $geneBioType,
			geneSymbol => $geneExternalName
			};
		
		
		
		# Get all of the probesets for this gene by reading from Affy Probeset Tables in database
		# We just have to read the probe sets once.
	    
		    my ($probesetHOHRef)
				= readAffyProbesetDataFromDB($geneChrom,$geneStart,$geneStop,$arrayTypeID,$dataSetID,$genomeVer,$dsn,$usr,$passwd);
		    my @probesetHOH = @$probesetHOHRef;
		    #print "get probesets\n";
		    #Get the transcripts for this gene
		    my $transcripts = $gene->get_all_Transcripts();

		    $cntTranscripts = 0;
		    while ( my $transcript = shift @{$transcripts} ) {
			my ($transcriptName, $transcriptRegion, $transcriptStart, $transcriptStop,$transcriptStrand) = getFeatureInfo($transcript);
			my $transcriptChrom = "chr$transcriptRegion";

			$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{start} = $transcriptStart;
			$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{stop} = $transcriptStop;
			$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{ID} = $transcriptName;
			$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{strand} = $transcriptStrand;
			$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{chromosome} = $transcriptChrom;
			$cntExons = 0;
			$cntIntrons=0;
			
			# On to the exons
			#sort first so introns can be created as we go
			my @tmpExons= @{ $transcript->get_all_Exons() };
			my @sortedExons = sort { $a->seq_region_start() <=> $b->seq_region_start() } @tmpExons;
			
		    foreach my $exon ( @sortedExons ) {
				my ($exonName, $exonRegion, $exonStart, $exonStop,$exonStrand) = getFeatureInfo($exon);
				#print "get Exons\n";
				my $exonChrom = "chr$exonRegion";
				# have to offset the stop and start by the slice start
				#print "test1".$exon->coding_region_end($transcript)."\n";
				#print "test2".$slice->start()."\n";
				
				my $coding_region_stop = $exon->coding_region_end($transcript) + $slice->start() - 1;
				my $coding_region_start = $exon->coding_region_start($transcript) + $slice->start() - 1;
				$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{start} = $exonStart;
				$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{stop} = $exonStop;
				$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{ID} = $exonName;
				$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{strand} = $exonStrand;
				$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{chromosome} = $exonChrom;
				$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{coding_start} = $coding_region_start;
				$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{coding_stop} = $coding_region_stop;
			
				my $intronStart=-1;
				my $intronStop=-1;
				#create intronList
				if($cntExons>0){
				    $intronStart=$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons-1]{stop}+1;
				    $intronStop=$GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{start}-1;
				    
				    $GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{intronList}{intron}[$cntIntrons]{start} = $intronStart;
				    $GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{intronList}{intron}[$cntIntrons]{stop} = $intronStop;
				    $GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{intronList}{intron}[$cntIntrons]{ID} = $cntIntrons+1;
				    $cntIntrons=$cntIntrons+1;
				}
			
			
			#Now find which probesets are associated with each exon	and intron
			#Check if the probeset location overlaps the exon location
			#if it is not over an exon check to see if it is over an intron
			
				$cntProbesets=0;
				$cntMatchingProbesets=0;
				$cntMatchingIntronProbesets=0;
				foreach(@probesetHOH){
					if($exonStart<$exonStop){# if gene is in the forward direction
					    if(($probesetHOH[$cntProbesets]{start} >= $exonStart) and ($probesetHOH[$cntProbesets]{start} <= $exonStop) or 
					    ($probesetHOH[$cntProbesets]{stop} >= $exonStart) and ($probesetHOH[$cntProbesets]{stop} <= $exonStop))
					    {
						    #This is a probeset overlapping the current exon
						    $GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{ProbesetList}{Probeset}[$cntMatchingProbesets] = 
							    $probesetHOH[$cntProbesets];
						    $cntMatchingProbesets=$cntMatchingProbesets+1;
					    }elsif(($probesetHOH[$cntProbesets]{start} >= $intronStart) and ($probesetHOH[$cntProbesets]{start} <= $intronStop) or 
					    ($probesetHOH[$cntProbesets]{stop} >= $intronStart) and ($probesetHOH[$cntProbesets]{stop} <= $intronStop)){
						    $GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{intronList}{intron}[$cntIntrons-1]{ProbesetList}{Probeset}[$cntMatchingIntronProbesets] = 
							    $probesetHOH[$cntProbesets];
						    $cntMatchingIntronProbesets=$cntMatchingIntronProbesets+1;
					    }
					}else{# gene is in reverse direction
					    if(($probesetHOH[$cntProbesets]{start} <= $exonStart) and ($probesetHOH[$cntProbesets]{start} >= $exonStop) or 
					    ($probesetHOH[$cntProbesets]{stop} <= $exonStart) and ($probesetHOH[$cntProbesets]{stop} >= $exonStop))
					    {
						    #This is a probeset overlapping the current exon
						    $GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{exonList}{exon}[$cntExons]{ProbesetList}{Probeset}[$cntMatchingProbesets] = 
							    $probesetHOH[$cntProbesets];
						    $cntMatchingProbesets=$cntMatchingProbesets+1;
					    }elsif(($probesetHOH[$cntProbesets]{start} <= $intronStart) and ($probesetHOH[$cntProbesets]{start} >= $intronStop) or 
					    ($probesetHOH[$cntProbesets]{stop} <= $intronStart) and ($probesetHOH[$cntProbesets]{stop} >= $intronStop)){
						    $GeneHOH{Gene}[$cntGenes]{TranscriptList}{Transcript}[$cntTranscripts]{intronList}{intron}[$cntIntrons-1]{ProbesetList}{Probeset}[$cntMatchingIntronProbesets] = 
							    $probesetHOH[$cntProbesets];
						    $cntMatchingIntronProbesets=$cntMatchingIntronProbesets+1;
					    }
					}
					$cntProbesets = $cntProbesets+1;
				} # loop through probesets
				$cntExons=$cntExons+1;
		    } # loop through exons
		    $cntTranscripts = $cntTranscripts+1;
		} # loop through transcripts
		
		$cntGenes=$cntGenes+1;
	    }# if to process only if chromosome is valid
	} # loop through genes
	# create xml object	
	my $xml = new XML::Simple (RootName=>'GeneList');
	my $data = $xml->XMLout(\%GeneHOH);
	# open xml file
	open XMLFILE, $xmlOutputFileName or die " Could not open XML file $xmlOutputFileName for writing $!\n\n";
	# write the header 
	print XMLFILE '<?xml version="1.0" encoding="UTF-8"?>';
	print XMLFILE "\n\n";
	# Write the xml data
	print XMLFILE $data;
	close XMLFILE;
}
#
#	
	my $arg1 = '>'.$ARGV[0]; #xml file name
	my $arg2 = $ARGV[1]; #species
	my $arg3 = $ARGV[2]; #annotation level
	my $arg4 = $ARGV[3]; #Gene name list
	my $arg5 = $ARGV[4]; #user name
	my $arg6 = $ARGV[5]; #dataset id
	my $arg7 = $ARGV[6]; #array type id
	my $arg8 = $ARGV[7]; 
	my $arg9 = $ARGV[8]; 
	my $arg10= $ARGV[9]; 
	my $arg11= $ARGV[10];
	my $arg12= $ARGV[11];
	my $arg13= $ARGV[12];
	my $arg14=$ARGV[13];
	my $arg15=$ARGV[14];

	createXMLFile($arg1, $arg2, $arg3, $arg4, $arg5, $arg6, $arg7, $arg8, $arg9, $arg10, $arg11, $arg12, $arg13, $arg14, $arg15);

	
	# Example call:
	# perl writeXML.pl /Users/clemensl/TestingOutput/ /Users/clemensl/TestingOutput/ /Users/clemensl/TestingOutput/gene.xml Mouse Core ENSMUSG00000029064
1;
