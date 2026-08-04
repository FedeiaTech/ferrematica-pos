package com.fedeiatech.sistemagestionpyme.service;

import java.io.IOException;
import java.util.Map;

public interface PostgrestClient {

    PostgrestResponse post(String path, String jsonBody, Map<String, String> headers) throws IOException;
}
