package jexe.core;

import java.io.IOException;
import java.util.Map;

import jexe.core.JEXECore.Authentication;
import jexe.core.JEXECore.ConnectionInfo;
import jexe.core.JEXECore.JEXEException;

/**
 * Supplements JEXECore with process manipulation facilities.
 */
public class JEXEProcess {
    
    private JEXEProcess() {
    }
    
    /**
     * 
     * Attempts to execute the process described by the given ProcessInfo object on the machine
     * specified by the given ConnectionInfo object.
     * 
     * @param connectionInfo
     *            Information specifying a connection to the target machine
     * @param processCreationInfo
     *            Information describing the process to start on the target machine
     * @return Returns the JEXESVC process handle for the new process
     * @throws IOException
     * @throws JEXEException
     * 
     */
    public static String execute(ConnectionInfo connectionInfo,
            ProcessCreationInfo processCreationInfo) throws IOException, JEXEException {
        String result = JEXECore.transactCommand(connectionInfo, "exec "
                + processCreationInfo.authentication + " " + processCreationInfo.startingDirectory
                + " " + JEXECore.mapToString(processCreationInfo.environmentVariables) + " "
                + processCreationInfo.redirectFlags + " \"" + processCreationInfo.command + "\"");
        return result.equals("FAIL") ? null : result;
    }
    
    /**
     * 
     * Attempts to kill the process described by the given PID with the given exit code.
     * 
     * @param connectionInfo
     *            Information specifying a connection to the target machine
     * @param pid
     *            The PID of the process to kill
     * @param exitCode
     *            The exit code with which the process should exit
     * @throws IOException
     * @throws JEXEException
     * 
     */
    public static void kill(ConnectionInfo connectionInfo, int pid, int exitCode)
            throws IOException, JEXEException {
        if (JEXECore.transactCommand(connectionInfo, "kill " + exitCode + " " + pid).equals("FAIL")) {
            throw new JEXEException("Unable to terminate process with PID " + pid
                    + " with exit code " + exitCode);
        }
    }
    
    public static QueryInfoProcess[] queryProcesses(ConnectionInfo connectionInfo,
            QueryInfoProcess queryInfoProcess) {
        return null;
    }
    
    public static QueryInfoWindow[] queryWindows(ConnectionInfo connectionInfo,
            QueryInfoWindow queryInfoWindow) {
        return null;
    }
    
    /**
     * Contains the information necessary for the creation of a new process.
     */
    public static class ProcessCreationInfo {
        
        /**
         * This constant is provided for consistency. Specification in {@link #redirectFlags} will
         * have no effect.
         */
        public static final byte REDIRECT_NONE = 0x0;
        
        /**
         * Specify in {@link #redirectFlags} to redirect the process's standard input stream to
         * JEXE.
         */
        public static final byte REDIRECT_STDIN = 0x1;
        
        /**
         * Specify in {@link #redirectFlags} to redirect the process's standard output stream to
         * JEXE.
         */
        public static final byte REDIRECT_STDOUT = 0x2;
        
        /**
         * Specify in {@link #redirectFlags} to redirect the process's standard error stream to
         * JEXE.
         */
        public static final byte REDIRECT_STDERR = 0x4;
        
        /**
         * This constant is provided for convenience. Specification in {@link #redirectFlags} will
         * have the effect of {@link #REDIRECT_STDIN}, {@link #REDIRECT_STDOUT}, and
         * {@link #REDIRECT_STDERR} combined.
         */
        public static final byte REDIRECT_ALL = 0x7;
        
        /**
         * An {@link Authentication} object used when creating the process. If specified, the
         * process will be created under the represented user context. If not specified, the process
         * will be created under the user context of JEXESVC.
         */
        public Authentication authentication;
        
        /**
         * The starting directory for the process.
         */
        public String startingDirectory;
        
        /**
         * A Map&lt;String, String&gt; of environment variables to pass to the process.
         */
        public Map<String, String> environmentVariables;
        
        /**
         * Specifies which streams of the new process (STDIN, STDOUT, and/or STDERR) to redirect to
         * JEXE. This uses a simple bit-flag technique based on the constants
         * {@link #REDIRECT_STDIN}, {@link #REDIRECT_STDOUT}, and {@link #REDIRECT_STDERR}.
         */
        public byte redirectFlags;
        
        /**
         * The command to execute. The command is not processed by a shell; therefore, it is
         * strictly a space-separated set of values, the first of which being the executable name,
         * and the remaining being arguments to the program.
         */
        public String command;
        
    }
    
    public static class QueryInfoProcess {
        
        public int pid;
        public String name;
        public String path;
        public String domain;
        public String user;
        
    }
    
    public static class QueryInfoWindow {
        
        public String title;
        public int parentProcessPID;
        
    }
    
}
