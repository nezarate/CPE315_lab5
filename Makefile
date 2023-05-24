lab5: 
	javac lab5.java
	javac Instruction.java

run_tests:
	java lab5 lab4_fib20.asm lab5.script > out1
	diff -w -B lab5_ghr2.output out1
	java lab5 lab4_fib20.asm lab5.script 4 > out2
	diff -w -B lab5_ghr4.output out2
	java lab5 lab4_fib20.asm lab5.script 8 > out3
	diff -w -B lab5_ghr8.output out3

