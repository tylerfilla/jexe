package jexe.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;

import jcifs.dcerpc.DcerpcBinding;
import jcifs.dcerpc.msrpc.svcctl;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbNamedPipe;

public class JEXECore {
    
    private static final HashMap<String, JEXEProcess> processMap = new HashMap<String, JEXEProcess>();
    
    private JEXECore() {
    }
    
    public static JEXEProcess startProcess(ConnectionInfo connectionInfo, ProcessInfo processInfo)
            throws IOException {
        StringBuilder processCommandBuilder = new StringBuilder();
        processCommandBuilder.append("exec ");
        processCommandBuilder.append("\"").append(processInfo.executable).append("\" ");
        
        if (processInfo.interactive) {
            processCommandBuilder.append("i");
        }
        
        String result = "";
        try {
            result = transactCommand(connectionInfo, processCommandBuilder.toString());
        } catch (CommandException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static String transactCommand(ConnectionInfo connectionInfo, String command)
            throws CommandException, IOException {
        SmbNamedPipe commandPipe = new SmbNamedPipe("smb://" + connectionInfo.hostname
                + "/ipc$/pipe/jexesvc/cmd", SmbNamedPipe.PIPE_TYPE_RDWR,
                connectionInfo.authentication.toNtlmPasswordAuthentication());
        
        PrintWriter writer = new PrintWriter(commandPipe.getNamedPipeOutputStream());
        writer.print("COMMAND " + command + "\n");
        writer.flush();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                commandPipe.getNamedPipeInputStream()));
        
        String response = reader.readLine();
        
        if (response.startsWith("ERROR ") && response.length() > 6) {
            throw new CommandException("Response error: " + response.substring(6));
        } else if (response.startsWith("RESPONSE ") && response.length() > 9) {
            int numLines;
            try {
                numLines = Integer.parseInt(response.substring(9));
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid response", e);
            }
            
            StringBuilder responseBuilder = new StringBuilder();
            
            for (int i = 0; i < numLines; i++) {
                responseBuilder.append(reader.readLine()).append("\n");
            }
            
            response = responseBuilder.toString();
        }
        
        writer.close();
        reader.close();
        
        return response;
    }
    
    public static class ProcessInfo {
        
        public String id;
        public String executable;
        public Authentication authentication;
        public boolean interactive;
        
    }
    
    public static class ConnectionInfo {
        
        public String hostname;
        public Authentication authentication;
        
    }
    
    public static class Authentication {
        
        public String domain;
        public String username;
        public String password;
        
        public NtlmPasswordAuthentication toNtlmPasswordAuthentication() {
            return new NtlmPasswordAuthentication(this.domain, this.username, this.password);
        }
        
    }
    
    public static class CommandException extends Exception {
        
        private static final long serialVersionUID = -2761148477357758374L;
        
        public CommandException(String message) {
            super(message);
        }
        
        public CommandException(String message, Throwable cause) {
            super(message, cause);
        }
        
    }
    
    static {
        DcerpcBinding.addInterface("svcctl", svcctl.getSyntax());
    }
    
}
