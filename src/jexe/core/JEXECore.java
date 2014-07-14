package jexe.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import jcifs.dcerpc.DcerpcBinding;
import jcifs.dcerpc.msrpc.svcctl;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbNamedPipe;

/**
 * The core of JEXE; JEXECore is responsible for installing and uninstalling JEXESVC instances, and
 * for providing a method to communicate with these instances. JEXECore does not provide an
 * interface for process manipulation; {@link JEXEProcess} performs all process-related functions.
 */
public class JEXECore {
    
    static final String pipeUrlBase = "/jexesvc";
    static final String pipeUrlCommand = pipeUrlBase + "/cmd";
    static final String pipeUrlProcessStdin = pipeUrlBase + "/stdin";
    static final String pipeUrlProcessStdout = pipeUrlBase + "/stdout";
    static final String pipeUrlProcessStderr = pipeUrlBase + "/stderr";
    
    private JEXECore() {
    }
    
    /**
     * 
     * Installs and starts JEXESVC, a relatively small Win32 service that acts on the commands sent
     * by JEXE operations, to the machine specified by the given {@link ConnectionInfo} object. The
     * machine must have admin shares and file sharing enabled. JEXESVC must be installed on a given
     * machine before commands may be transacted, and by extension, before processes may be
     * manipulated.
     * 
     * @param connectionInfo
     *            Information specifying a connection to the target machine
     * @throws IOException
     * @throws JEXEException
     * 
     */
    public static void install(ConnectionInfo connectionInfo) throws IOException, JEXEException {
        
        // TODO: Copy the executable, connect to the SVCCTL pipe, and register the service on the target machine
        
    }
    
    /**
     * 
     * Stops and uninstalls JEXESVC from the machine specified by the given {@link ConnectionInfo}
     * object. JEXESVC must be installed for remote interactions to occur. Once interactions are
     * complete, however, it may be appropriate to remove JEXESVC from the target machine, depending
     * on the circumstances. The advantage to uninstalling JEXESVC is that the system is left in the
     * condition in which it existed before the remote operations took place. The disadvantage is
     * that JEXESVC must be reinstalled for future operations, which may take some time.
     * 
     * @param connectionInfo
     *            Information specifying a connection to the target machine
     * @throws IOException
     * @throws JEXEException
     * 
     */
    public static void uninstall(ConnectionInfo connectionInfo) throws IOException, JEXEException {
        
        // TODO: Connect to the SVCCTL pipe, unregister the service, and delete the executable on the target machine
        
    }
    
    /**
     * 
     * Checks for the presence of JEXESVC on the machine specified by the given
     * {@link ConnectionInfo} object. If this method returns false, remote operations should fail.
     * 
     * @param connectionInfo
     *            Information specifying a connection to the target machine
     * @return Whether or not JEXESVC was detected on the target machine
     * @throws IOException
     * 
     */
    public static boolean checkInstall(ConnectionInfo connectionInfo) {
        try {
            new SmbNamedPipe("smb://" + connectionInfo.hostname + "/ipc$/pipe" + pipeUrlCommand,
                    SmbNamedPipe.PIPE_TYPE_RDWR,
                    connectionInfo.authentication.toNtlmPasswordAuthentication());
        } catch (IOException e) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 
     * Performs a command transaction; that is, sends the given command to the JEXESVC instance on
     * the machine specified by the given {@link ConnectionInfo} object, and returns the result
     * received.
     * 
     * @param connectionInfo
     *            Information specifying a connection to the target machine
     * @param command
     *            The command to send to the target JEXESVC instance
     * @return The result of the command
     * @throws IOException
     * @throws JEXEException
     * 
     */
    public static String transactCommand(ConnectionInfo connectionInfo, String command)
            throws IOException, JEXEException {
        SmbNamedPipe commandPipe;
        try {
            commandPipe = new SmbNamedPipe("smb://" + connectionInfo.hostname + "/ipc$/pipe"
                    + pipeUrlCommand, SmbNamedPipe.PIPE_TYPE_RDWR,
                    connectionInfo.authentication.toNtlmPasswordAuthentication());
        } catch (IOException e) {
            throw new JEXEException("Unable to connect to JEXESVC on target machine", e);
        }
        
        PrintWriter writer = new PrintWriter(commandPipe.getNamedPipeOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                commandPipe.getNamedPipeInputStream()));
        
        writer.print("COMMAND " + command + "\n");
        writer.flush();
        
        String response = reader.readLine();
        
        if (response.startsWith("RESPONSE ")) {
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
                        responseBuilder.append(line);
                        
                        if (i < numLines - 1) {
                            responseBuilder.append("\n");
                        }
                    }
                }
                
                response = responseBuilder.toString();
            } else {
                throw new CommandException("Protocol: response line count not specified");
            }
        } else if (response.startsWith("ERROR ")) {
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
    
    /**
     * 
     * Converts a Map&lt;String, String&gt; to a JEXESVC map.
     * 
     * @param map
     *            The Map<String, String> to convert
     * @return The resultant String
     * 
     */
    public static String mapToString(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        
        builder.append("[");
        
        int count = 0;
        for (String key : map.keySet()) {
            String value = map.get(key);
            
            if (key.contains(" ") || key.contains(",") || key.contains("=")) {
                builder.append("\"").append(key).append("\"");
            } else {
                builder.append(key);
            }
            
            builder.append("=");
            
            if (value.contains(" ") || value.contains(",") || value.contains("=")) {
                builder.append("\"").append(value).append("\"");
            } else {
                builder.append(value);
            }
            
            if (count < map.size() - 1) {
                builder.append(",");
            }
            
            count++;
        }
        
        builder.append("]");
        
        return builder.toString();
    }
    
    /**
     * 
     * Converts a JEXESVC map to a Map&lt;String, String&gt;.
     * 
     * @param string
     *            The JEXESVC map to convert
     * @return The resultant Map&lt;String, String&gt;
     * 
     */
    public static Map<String, String> stringToMap(String string) {
        Map<String, String> map = new HashMap<String, String>();
        
        boolean open = false;
        boolean put = false;
        boolean stage = false;
        
        boolean quote = false;
        boolean escape = false;
        
        String workingKey = "";
        String workingValue = "";
        
        for (int ci = 0; ci < string.length(); ci++) {
            char c = string.charAt(ci);
            
            if (c == '[' && !quote && !open) {
                open = true;
            } else if (c == ']' && !quote && open) {
                open = false;
                put = true;
            } else if (open) {
                if (c == '=' && !quote) {
                    stage = true;
                } else if (c == ',' && !quote) {
                    stage = false;
                    put = true;
                } else {
                    if (c == '\\') {
                        escape = true;
                    } else {
                        escape = false;
                    }
                    
                    if (c == '"' && !escape) {
                        quote = !quote;
                    } else {
                        if (stage) {
                            workingValue += c;
                        } else {
                            workingKey += c;
                        }
                    }
                }
            }
            
            if (put) {
                map.put(workingKey, workingValue);
                
                workingKey = "";
                workingValue = "";
                
                put = false;
            }
        }
        
        return map;
    }
    
    /**
     * Contains information for connecting to another device, including a hostname/address and an
     * {@link Authentication} object.
     */
    public static class ConnectionInfo {
        
        /**
         * The hostname of the device to which to connect.
         */
        public String hostname;
        
        /**
         * An {@link Authentication} object used for connection to the device. This does not affect
         * processes; process authentication is handled in {@link JEXEProcess}.
         */
        public Authentication authentication;
        
    }
    
    /**
     * Represents a set of authentication credentials.
     */
    public static class Authentication {
        
        /**
         * This field is optional, but may be required.
         */
        public String domain;
        
        /**
         * The username with which to authenticate.
         */
        public String username;
        
        /**
         * The password with which to authenticate.
         */
        public String password;
        
        /**
         * 
         * Returns a JCIFS {@link NtlmPasswordAuthentication} object representing the same
         * credentials stored in this Authentication.
         * 
         * @return The NtlmPasswordAuthentication object
         * 
         */
        NtlmPasswordAuthentication toNtlmPasswordAuthentication() {
            return new NtlmPasswordAuthentication(this.domain, this.username, this.password);
        }
        
        @Override
        public String toString() {
            return this.domain + "\\" + this.username + ":" + this.password;
        }
        
    }
    
    /**
     * Represents any JEXE-specific exception.
     */
    public static class JEXEException extends Exception {
        
        private static final long serialVersionUID = 2675859985742351255L;
        
        public JEXEException(String message) {
            super(message);
        }
        
        public JEXEException(String message, Throwable cause) {
            super(message, cause);
        }
        
    }
    
    /**
     * Represents any JEXE-command-specific exception.
     */
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
