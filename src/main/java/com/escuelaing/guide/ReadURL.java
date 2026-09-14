package com.escuelaing.guide;

import java.net.URI;
import java.net.URL;


public class ReadURL {

    public static void main(String[] args){
        try{
            URL myURL = new URI("http://ldbn.escuelaing.edu.co:8080/api/careers.pdf?career=ing-sistemas#projects").toURL();
            System.out.println("Protocol: " + myURL.getProtocol());
            System.out.println("Authority: " + myURL.getAuthority());
            System.out.println("Host: " + myURL.getHost());
            System.out.println("Port: " + myURL.getPort());
            System.out.println("Path: " + myURL.getPath());
            System.out.println("Query: " + myURL.getQuery());
            System.out.println("File: " + myURL.getFile());
            System.out.println("Ref: " + myURL.getRef());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
