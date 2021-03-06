=head1 NAME

OPOSSUM::ZscoreResultSet.pm - module to hold the results of a Z-score analysis

=head1 DESCRIPTION

Implements a set of ZscoreResult objects

=head1 AUTHOR

 David Arenillas
 Wasserman Lab
 Centre for Molecular Medicine and Therapeutics
 University of British Columbia

 E-mail: dave@cmmt.ubc.ca

=head1 METHODS

=cut

package OPOSSUM::Analysis::ZscoreResultSet;

use strict;

use Carp;

use OPOSSUM::Analysis::ZscoreResult;

=head2 new

 Title    : new
 Usage    : $zrs = OPOSSUM::Analysis::ZscoreResultSet->new();
 Function : Create a new OPOSSUM::Analysis::ZscoreResultSet object.
 Returns  : An OPOSSUM::Analysis::ZscoreResultSet object.
 Args     : None.

=cut

sub new
{
    my ($class, %args) = @_;

    my $self = bless {
    			_result_array	=> [],
			%args
		    }, ref $class || $class;

    return $self;
}

=head2 param

 Title    : param
 Usage    : $value = $zrs->param($param) or $zrs->param($param, $value);
 Function : Get/set a parameter value
 Returns  : A parameter value
 Args     : [1] a parameter name,
 	    [2] on set, a parameter value

=cut

sub param
{
    my ($self, $param, $value) = @_;

    if ($param) {
	if (defined $value) {
	    $self->{_params}->{$param} = $value;
	}
	return $self->{_params}->{$param};
    }
    return keys %{$self->{_params}};
}

=head2 num_results

 Title    : num_results
 Usage    : $num = $zrs->num_results();
 Function : Return the number of results in the set
 Returns  : An integer
 Args     : None

=cut

sub num_results
{
    return @{$_[0]->{_result_array}} ? scalar @{$_[0]->{_result_array}} : 0;
}

=head2 add_result

 Title    : add_result
 Usage    : $zrs->add_result($result);
 Function : Add a new result to the set
 Returns  : Nothing
 Args     : An OPOSSUM::Analysis::ZscoreResult object

=cut

sub add_result
{
    my ($self, $new_result) = @_;

    return if !defined $new_result;

    if (!$new_result->isa("OPOSSUM::Analysis::ZscoreResult")) {
    	carp "not an OPOSSUM::Analysis::ZscoreResult";
	return;
    }

    my $new_id = $new_result->id;
    foreach my $result (@{$self->{_result_array}}) {
	if ($new_id eq $result->id) {
	    carp "result with ID $new_id already exists in set";
	    return;
	}
    }

    push @{$self->{_result_array}}, $new_result;

    return $new_result;
}

=head2 get_result

 Title    : get_result
 Usage    : $result = $zrs->get_result($idx);
 Function : Return a result from the set by it's index in the set
 Returns  : An OPOSSUM::Analysis::ZscoreResult object
 Args     : Index of the result in the set

=cut

sub get_result
{
    my ($self, $idx) = @_;

    return if !defined $idx;

    return if $idx >= $self->num_results;

    return $self->{_result_array}->[$idx];
}

=head2 get_result_by_id

 Title    : get_result_by_id
 Usage    : $result = $zrs->get_result_by_id($id);
 Function : Return a result from the set by it's unique ID
 Returns  : An OPOSSUM::Analysis::ZscoreResult object
 Args     : ID of the result in the set

=cut

sub get_result_by_id
{
    my ($self, $id) = @_;

    return if !defined $id;

    foreach my $result (@{$self->{_result_array}}) {
    	if ($result->id eq $id) {
	    return $result;
	}
    }
    return undef;
}

=head2 sort_by

 Title    : sort_by
 Usage    : $zrs->sort_by($field, $reverse);
 Function : Sort the result set by the given field
 Returns  : Nothing
 Args     : [1] an OPOSSUM::Analysis::ZscoreResult field to sort the set on
	    [2] a boolean which, if true, causes the sort to be reversed

=cut

sub sort_by
{
    my ($self, $field, $reverse) = @_;

    return if !defined $field;

    return if !@{$self->{_result_array}};

    if ($reverse) {
	$self->{_result_array} = [sort {$b->$field <=> $a->$field}
    					@{$self->{_result_array}}];
    } else {
	$self->{_result_array} = [sort {$a->$field <=> $b->$field}
    					@{$self->{_result_array}}];
    }
}

1;
