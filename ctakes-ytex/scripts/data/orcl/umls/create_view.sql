create table v_snomed_fword_lookup (
  cui char(8) not null,
  tui char(8) null,
  fword varchar2(70) not null,
  fstem varchar2(70) null,
  tok_str varchar2(250) not null,
  stem_str varchar2(250) null
)
;
