package jexe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import jexe.core.JEXECore;
import jexe.core.JEXEProcess;

public class JEXE {
    
    public static void main(String[] args) {
        System.out.println("JEXE: Remote process management for Windows systems");
        System.out.println("Written by Tyler Filla");
        
        String pass = "";
        
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Input password: ");
            pass = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        JEXECore.Authentication authentication = new JEXECore.Authentication();
        authentication.domain = "TYLER-LAPTOP";
        authentication.username = "Tyler";
        authentication.password = pass;
        
        JEXECore.ConnectionInfo connectionInfo = new JEXECore.ConnectionInfo();
        connectionInfo.hostname = "TYLER-LAPTOP";
        connectionInfo.authentication = authentication;
        
        JEXEProcess.ProcessCreationInfo processInfo = new JEXEProcess.ProcessCreationInfo();
        processInfo.authentication = authentication;
        processInfo.startingDirectory = "C:\\Users\\Tyler";
        processInfo.environmentVariables = new HashMap<String, String>();
        processInfo.environmentVariables.put("key1", "value1");
        processInfo.environmentVariables.put("key2", "value2");
        processInfo.environmentVariables.put("key3", "value3");
        processInfo.redirectFlags = JEXEProcess.ProcessCreationInfo.REDIRECT_STDOUT;
        processInfo.command = "notepad.exe C:\\Users\\Tyler\\.gitconfig";
        
        try {
            JEXECore.install(connectionInfo);
            
            JEXEProcess.QueryInfoProcess query = new JEXEProcess.QueryInfoProcess();
            query.name = "notepad.exe";
            query.user = "Tyler";
            
            JEXEProcess.QueryInfoProcess[] results = JEXEProcess.queryProcesses(connectionInfo,
                    query);
            
            for (JEXEProcess.QueryInfoProcess result : results) {
                JEXEProcess.kill(connectionInfo, result.pid, 0);
            }
            
            JEXECore.uninstall(connectionInfo);
        } catch (IOException | JEXECore.JEXEException e) {
            e.printStackTrace();
        }
    }
    
}
