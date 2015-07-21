// Copyright (c) 2015 Matt Weagle (mweagle@gmail.com)

// Permission is hereby granted, free of charge, to
// any person obtaining a copy of this software and
// associated documentation files (the "Software"),
// to deal in the Software without restriction,
// including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so,
// subject to the following conditions:

// The above copyright notice and this permission
// notice shall be included in all copies or substantial
// portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
// ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
// SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
package com.mweagle;

import com.mweagle.tereus.commands.CreateCommand;
import com.mweagle.tereus.commands.DeleteCommand;
import com.mweagle.tereus.commands.GuiCommand;

import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import io.airlift.airline.Help;;

public class Tereus {

	@SuppressWarnings("unchecked")
    public static void main(String[] args) {   
    	
		CliBuilder<Runnable> builder = Cli.<Runnable>builder("Tereus")
                .withDescription("Executable CloudFormation definitions")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class, CreateCommand.class, DeleteCommand.class);

        builder.withGroup("cli")
                .withDescription("Command line mode")
                .withDefaultCommand(CreateCommand.class)
                .withCommands(CreateCommand.class, DeleteCommand.class);

        builder.withGroup("gui")
		        .withDescription("Localhost GUI mode")
		        .withDefaultCommand(GuiCommand.class)
		        .withCommands(GuiCommand.class);
        
        Cli<Runnable> tereusParser = builder.build();

        tereusParser.parse(args).run();
    }
}
