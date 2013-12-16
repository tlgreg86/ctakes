CREATE CACHED TABLE v_snomed_fword_lookup (
  cui char(8) not null,
  tui char(8) null,
  fword varchar(70) not null,
  fstem varchar(70) not null,
  tok_str varchar(250) not null,
  stem_str varchar(250) not null
);
