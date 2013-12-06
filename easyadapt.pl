for ($i=0; $i<@ARGV; $i++) {
    open F, $ARGV[$i];
    while (<F>) {
        chomp;
        ($y,@x) = split;
        print $y;
        map { print " *$_ $i$_" } @x;
        print "\n";
    }
}
