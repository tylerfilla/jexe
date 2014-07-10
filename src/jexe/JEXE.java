package jexe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jexe.core.JEXECore;

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
        
        try {
            System.out.println(JEXECore.transactCommand(connectionInfo, "testing...!"));
        } catch (JEXECore.CommandException | IOException e) {
            e.printStackTrace();
        }
    }
    
}
