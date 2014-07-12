package jexe.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import jcifs.dcerpc.DcerpcBinding;
import jcifs.dcerpc.msrpc.svcctl;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbNamedPipe;

public class JEXECore {
    
    private static final String pipeUrlBase = "/jexesvc";
    private static final String pipeUrlCommand = pipeUrlBase + "/cmd";
    private static final String pipeUrlProcessStdin = pipeUrlBase + "/stdin";
    private static final String pipeUrlProcessStdout = pipeUrlBase + "/stdout";
    private static final String pipeUrlProcessStderr = pipeUrlBase + "/stderr";
    
    private JEXECore() {
    }
    
    public static void install(ConnectionInfo connectionInfo) throws IOException, JEXEException {
    }
    
    public static void uninstall(ConnectionInfo connectionInfo) throws IOException, JEXEException {
    }
    
    public static String transactCommand(ConnectionInfo connectionInfo, String command)
            throws IOException, JEXEException {
        SmbNamedPipe commandPipe = new SmbNamedPipe("smb://" + connectionInfo.hostname
                + "/ipc$/pipe" + pipeUrlCommand, SmbNamedPipe.PIPE_TYPE_RDWR,
                connectionInfo.authentication.toNtlmPasswordAuthentication());
        
        PrintWriter writer = new PrintWriter(commandPipe.getNamedPipeOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                commandPipe.getNamedPipeInputStream()));
        
        writer.print("COMMAND " + command + "\n");
        writer.flush();
        
        String response = reader.readLine();
        
        if (response.startsWith("RESPONSE")) {
            if (response.length() > 9) {
                int numLines;
                try {
                    numLines = Integer.parseInt(response.substring(9));
                } catch (NumberFormatException e) {
                    throw new CommandException("Protocol: invalid response line count", e);
                }
                
                StringBuilder responseBuilder = new StringBuilder();
                
                for (int i = 0; i < numLines; i++) {
                    String line = reader.readLine();
                    
                    if (!line.isEmpty()) {
                        responseBuilder.append(reader.readLine());
                        
                        if (i < numLines - 1) {
                            responseBuilder.append("\n");
                        }
                    }
                }
                
                response = responseBuilder.toString();
            } else {
                throw new CommandException("Protocol: response line count not specified");
            }
        } else if (response.startsWith("ERROR")) {
            if (response.length() > 6) {
                throw new CommandException("Response: " + response.substring(6));
            } else {
                throw new CommandException("Protocol: error code not specified");
            }
        } else {
            throw new CommandException("Protocol: unrecognized response");
        }
        
        writer.close();
        reader.close();
        
        return response;
    }
    
    public static Process execProcess(ConnectionInfo connectionInfo, ProcessInfo processInfo)
            throws IOException, JEXEException {
        return null;
    }
    
    public static class ConnectionInfo {
        
        public String hostname;
        public Authentication authentication;
        
    }
    
    public static class Authentication {
        
        public String domain;
        public String username;
        public String password;
        
        private NtlmPasswordAuthentication toNtlmPasswordAuthentication() {
            return new NtlmPasswordAuthentication(this.domain, this.username, this.password);
        }
        
    }
    
    public static class ProcessInfo {
        
        public static final byte REDIRECT_STDIN = 0x1;
        public static final byte REDIRECT_STDOUT = 0x2;
        public static final byte REDIRECT_STDERR = 0x4;
        
        public String command;
        public byte redirectFlags;
        
    }
    
    public static class Process {
        
        private String processId;
        private ProcessInfo processInfo;
        
        private ConnectionInfo connectionInfo;
        
        private InputStream stdin;
        private OutputStream stdout;
        private InputStream stderr;
        
        private Process() {
        }
        
        public void kill() throws IOException, JEXEException {
        }
        
        public InputStream getInputStream() throws IOException, JEXEException {
            if (this.stdin == null) {
                this.stdin = new SmbNamedPipe("smb://" + this.connectionInfo.hostname
                        + "/ipc$/pipe" + pipeUrlProcessStdin + "/" + this.processId,
                        SmbNamedPipe.PIPE_TYPE_RDONLY,
                        this.connectionInfo.authentication.toNtlmPasswordAuthentication())
                        .getNamedPipeInputStream();
            }
            
            return this.stdin;
        }
        
        public OutputStream getOutputStream() throws IOException, JEXEException {
            if (this.stdout == null) {
                this.stdout = new SmbNamedPipe("smb://" + this.connectionInfo.hostname
                        + "/ipc$/pipe" + pipeUrlProcessStdout + "/" + this.processId,
                        SmbNamedPipe.PIPE_TYPE_WRONLY,
                        this.connectionInfo.authentication.toNtlmPasswordAuthentication())
                        .getNamedPipeOutputStream();
            }
            
            return this.stdout;
        }
        
        public InputStream getErrorStream() throws IOException, JEXEException {
            if (this.stderr == null) {
                this.stderr = new SmbNamedPipe("smb://" + this.connectionInfo.hostname
                        + "/ipc$/pipe" + pipeUrlProcessStderr + "/" + this.processId,
                        SmbNamedPipe.PIPE_TYPE_RDONLY,
                        this.connectionInfo.authentication.toNtlmPasswordAuthentication())
                        .getNamedPipeInputStream();
            }
            
            return this.stderr;
        }
        
    }
    
    public static class JEXEException extends Exception {
        
        private static final long serialVersionUID = 2675859985742351255L;
        
        public JEXEException(String message) {
            super(message);
        }
        
        public JEXEException(String message, Throwable cause) {
            super(message, cause);
        }
        
    }
    
    public static class CommandException extends JEXEException {
        
        private static final long serialVersionUID = -5987625057248628173L;
        
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
