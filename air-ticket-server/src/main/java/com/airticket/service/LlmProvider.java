package com.airticket.service;
import com.fasterxml.jackson.databind.JsonNode;
public interface LlmProvider {
 boolean available();
 JsonNode json(String system,String input,long deadlineNanos);
}
