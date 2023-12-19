package it.dt.function.augmentation;

import it.dt.function.exception.CommandLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 30/09/2020 - 21:34
 */
public class LinuxCliExecutor implements CommandLineExecutor {

    private static final Logger logger = LoggerFactory.getLogger(LinuxCliExecutor.class);

    public CommandLineResult executeCommand(String command) throws CommandLineException {

        try{

            logger.info("Executing command: {}", command);

            Runtime runtime = Runtime.getRuntime();
            Process pr = runtime.exec(command);

            pr.waitFor();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader errorBufferReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            String line = null;
            String errorLine = null;

            StringBuilder consoleLog = new StringBuilder();
            StringBuilder errorLog = new StringBuilder();

            while((line = bufferedReader.readLine()) != null || (errorLine = errorBufferReader.readLine()) != null) {
                if(line != null) consoleLog.append(line).append('\n');
                if(errorLine != null) errorLog.append(errorLine).append('\n');
            }

            return new CommandLineResult(pr.exitValue(), consoleLog.toString(), errorLog.toString());

        }catch (Exception e){
            throw new CommandLineException(String.format("Error Executing Command: %s Error: %s", command, e.getLocalizedMessage()));
        }

    }

    @Override
    public CommandLineResult executeCommand(String command, List<String> parameterList, String targetDirectory) throws CommandLineException {
        throw new CommandLineException("Method not supported !");
    }


    public static void main(String[] args) {

        try{

            //String command = "keytool -list -keystore example.client.chain.p12 -storepass changeit";

            String command = "python3 scripts/csvReaderEcg.py data/AriannaC_100Hz.csv data/AriannaC_100Hz_stressors.csv data/test1.txt data/test2.txt";

            CommandLineExecutor commandLineExecutor = new LinuxCliExecutor();
            CommandLineResult cliResult = commandLineExecutor.executeCommand(command);

            logger.debug("############ Cli Command Line Result #############");
            logger.info("Command Result: {}", cliResult.getExitCode());
            logger.info("Error Log: {}", cliResult.getErrorLog());
            logger.info("Console Log: {}", cliResult.getOutputLog());

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
