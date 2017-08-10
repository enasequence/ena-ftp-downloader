package uk.ac.ebi.ena.downloader.model;

public class DownloadSettings {

    public DownloadSettings(Method ftp) {
        this.method = ftp;
    }

    public DownloadSettings(Method aspera, String executable, String sshText, String paramsText) {
        this.method = aspera;
        this.executable = executable;
        this.certificate = sshText;
        this.parameters = paramsText;
    }

    public enum Method {
        FTP, ASPERA
    }

    private String executable, certificate, parameters;
    private Method method;

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
