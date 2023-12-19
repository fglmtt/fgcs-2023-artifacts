package it.dt.function.augmentation;

import it.dt.function.exception.CommandLineException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project java-python-cli-executor
 * @created 19/04/2023 - 18:02
 */
public class PythonCliExecutor extends LinuxCliExecutor {

    private String pythonCommandPath;

    public PythonCliExecutor(String pythonCommandPath){
        this.pythonCommandPath = pythonCommandPath;
    }

    public CommandLineResult executePythonCommand(String scriptPath, List<String> commandParametersList) throws CommandLineException {

        String parameterResultString = commandParametersList.stream().collect(Collectors.joining(" ", "", ""));
        String finalCommand = String.format("%s %s %s", this.pythonCommandPath, scriptPath, parameterResultString);
        return this.executeCommand(finalCommand);
    }

    public String getPythonCommandPath() {
        return pythonCommandPath;
    }

    public void setPythonCommandPath(String pythonCommandPath) {
        this.pythonCommandPath = pythonCommandPath;
    }

    public static void main(String[] args) {
        try{

            PythonCliExecutor pythonCliExecutor = new PythonCliExecutor("python3");
            CommandLineResult commandLineResult = pythonCliExecutor.executePythonCommand("functions/prime_number_function.py", Arrays.asList("100"));

            if(commandLineResult.getExitCode() >= 0)
                System.out.println("Result:" + commandLineResult.getOutputLog());
            else
                System.err.println("Error ! Exit Code:" + commandLineResult.getExitCode());

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
