package jexe.core;

import jcifs.dcerpc.DcerpcBinding;
import jcifs.dcerpc.msrpc.svcctl;
import jcifs.smb.NtlmPasswordAuthentication;

public class JEXECore {
    
    static {
        DcerpcBinding.addInterface("svcctl", svcctl.getSyntax());
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
    
}
