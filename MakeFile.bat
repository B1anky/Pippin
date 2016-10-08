javac -d . -classpath .;junit.jar;hamcrest.jar *.java
java -cp .;junit.jar;hamcrest.jar org.junit.runner.JUnitCore pippin.InstructionTester
pause