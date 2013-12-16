create table $(db_schema).v_snomed_fword_lookup (
	cui char(8), 
	tui char(4), 
	fword nvarchar(70), 
	fstem nvarchar(70), 
	tok_str nvarchar(250), 
	stem_str nvarchar(250)
)
;
