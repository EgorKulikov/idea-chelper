![http://imageshack.us/a/img855/7161/projectc.png](http://imageshack.us/a/img855/7161/projectc.png)

Makes project compatible with CHelper - you can select some settings and it also adds required libraries. All directories below are relative to project root. Recomended value for project created with default settings in parenthes.

Default directory (src/myPackage) - directory where created, parsed or [TopCoder](http://topcoder.com/tc) tasks are by default. Should be under source in **[non-default package](http://www.xyzws.com/Javafaq/what-is-the-default-package/126)**.

Archive directory (archive) - root directory for archives of past contests. It is recommended to be not under source directory.

Output directory (src) - directory where target source file will be created. Should be under source in **[default package](http://www.xyzws.com/Javafaq/what-is-the-default-package/126)**.

Enable unit tests - check if you would like to create unit tests for your library based on archived tasks.

Test directory (lib/test) - root directory for unit tests. Should be under test source and in **[default package](http://www.xyzws.com/Javafaq/what-is-the-default-package/126)**.

Input class (java.util.Scanner) - class to be used as reader. Should have public constructor that accepts InputStream and public method String next() that returns next token.

Output class (java.io.PrintWriter) - class to be used as writer. Should have two public constructors accepting OutputStream and Writer respectively and public method void close()

Exclude packages (java.,javax.,com.sun.) - classes from this packages and their descendants would not be inlined. They should be available in server enviroment.

Author - content of author tag in target source file.