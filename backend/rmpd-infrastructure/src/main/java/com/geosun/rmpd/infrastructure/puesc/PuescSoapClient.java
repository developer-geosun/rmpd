package com.geosun.rmpd.infrastructure.puesc;

public interface PuescSoapClient {

    ConnectionTestResult testConnection(String username, String password);

    AcceptDocumentResult acceptDocument(String username, String password, byte[] xmlContent, String filename);

    PuescIncomingDocument getNextDocument(String username, String password, String targetSystem);
}
