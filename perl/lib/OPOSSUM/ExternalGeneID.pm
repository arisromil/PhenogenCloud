=head1 NAME

OPOSSUM::ExternalGeneID - ExternalGeneID object (external_gene_ids DB record)

=head1 DESCRIPTION

An ExternalGeneID models a record retrieved from the external_gene_ids table
of the oPOSSUM DB.

=head1 AUTHOR

 David Arenillas
 Wasserman Lab
 Centre for Molecular Medicine and Therapeutics
 University of British Columbia

 E-mail: dave@cmmt.ubc.ca

=head1 METHODS

=cut
package OPOSSUM::ExternalGeneID;

use strict;

use Carp;

=head2 new

 Title   : new
 Usage   : $xgid = OPOSSUM::ExternalGeneID->new(
			    -gene_pair_id	=> '28356',
			    -id_type		=> 1,
			    -species		=> 1,
			    -external_id	=> 'ABCA1');

 Function: Construct a new ExternalGeneID object
 Returns : a new OPOSSUM::ExternalGeneID object

=cut

sub new
{
    my ($class, %args) = @_;

    my $self = bless {
		    %args
		}, ref $class || $class;

    return $self;
}

=head2 gene_pair_id

 Title   : gene_pair_id
 Usage   : $id = $xgid->gene_pair_id() or $xgid->gene_pair_id($id);

 Function: Get/set the gene pair ID associated with this external gene ID.
 Returns : A string
 Args    : None or an id string

=cut

sub gene_pair_id
{
    my ($self, $gene_pair_id) = @_;

    if (defined $gene_pair_id) {
	$self->{gene_pair_id} = $gene_pair_id;
    }
    return $self->{gene_pair_id};
}

=head2 id_type

 Title   : id_type
 Usage   : $type = $xgid->id_type() or $xgid->id_type($type);

 Function: Get/set the identifier type for this external gene pair
 	   identifier.
 Returns : An integer.
 Args    : None or a new id type.

=cut

sub id_type
{
    my ($self, $id_type) = @_;

    if (defined $id_type) {
	$self->{id_type} = $id_type;
    }
    return $self->{id_type};
}

=head2 species

 Title   : species
 Usage   : $species = $xgid->species() or $xgid->species($species);

 Function: Get/set the species ID for this external gene pair identifier.
 Returns : An integer.
 Args    : None or a new species id.

=cut

sub species
{
    my ($self, $species) = @_;

    if (defined $species) {
	$self->{species} = $species;
    }
    return $self->{species};
}

=head2 external_id

 Title   : external_id
 Usage   : $id = $xgid->external_id() or $xgid->external_id($id);

 Function: Get/set the external gene ID.
 Returns : A string
 Args    : None or an id string

=cut

sub external_id
{
    my ($self, $external_id) = @_;

    if (defined $external_id) {
	$self->{external_id} = $external_id;
    }
    return $self->{external_id};
}

1;
