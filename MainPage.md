# Installation #

Select menu item "File->Settings...", select "Plugins" in "IDE Settings", push "Browse repositories..." button, right click on CHelper and select "Download and Install". Click "Yes", "OK", "Apply" and "Restart".

Right click on main toolbar, select "Customize Menus and Toolbars...", select location where you want actions to be listed (probably end of "Main Toolbar" is good), click "Add After...", select "Plug-ins->CHelper" and add actions you are interested in (probably all but "Task")

After that open or create project that you want to use with plugin and click on "Edit Project Settings" to set-up project directories (more on this [here](EditProjectSettings.md))

# Actions #

![http://img19.imageshack.us/img19/9779/toolbarh.png](http://img19.imageshack.us/img19/9779/toolbarh.png)

  * [New Task](NewTask.md) - creates new task
  * [Edit Tests](EditTests.md) - specify tests to run your task on
  * [Copy Source](CopySource.md) - copy full source (i. e. one you may submit to server) to the clipboard
  * [Archive Task](ArchiveTask.md) - after you are done editing task you may put it to archive with associated files
  * [Restore Task](RestoreTask.md) - if you archived task by mistake or want to use some task from archive you may restore it
  * [Delete Task](DeleteTask.md) - if you are done with the task but do not want to archive it you may delete task and associated files
  * [Parse Contest](ParseContest.md) - you may create tasks for some sites automatically
  * [Edit Project Settings](EditProjectSettings.md) - edit project structure to use with plugin as well as some other settings like content of @author tag
  * [Launch TopCoder Arena](LaunchTopCoderArena.md) - launches [TopCoder](http://topcoder.com/tc) with it set up for use with Idea

# General usage guidelines #

  * If you want full source code to be (re)generated you need to run or debug task
  * [TopCoder](http://topcoder.com/tc) arena has very poor algorithm to detect unused code - it seems it considers any top-level classes beside task class as unused code. Warning provided by arena is for information purposes only. Still it may be a good idea to look at submitted code - remember, commented code is not deleted by plugin
  * CodeChef sample test cases are parsed using heuristics. They may be not parsed at all or parsed incorrectly