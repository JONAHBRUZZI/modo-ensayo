package com.modoensayo.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageConfig {
    private String provider = "local";
    private String uploadDir = "uploads";
    private String supabaseUrl;
    private String supabaseKey;
    private String supabaseBucket;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public String getSupabaseUrl() { return supabaseUrl; }
    public void setSupabaseUrl(String supabaseUrl) { this.supabaseUrl = supabaseUrl; }
    public String getSupabaseKey() { return supabaseKey; }
    public void setSupabaseKey(String supabaseKey) { this.supabaseKey = supabaseKey; }
    public String getSupabaseBucket() { return supabaseBucket; }
    public void setSupabaseBucket(String supabaseBucket) { this.supabaseBucket = supabaseBucket; }
}
