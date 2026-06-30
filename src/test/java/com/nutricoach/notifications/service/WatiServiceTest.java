package com.nutricoach.notifications.service;

import com.nutricoach.common.config.WatiProperties;
import com.nutricoach.common.exception.NutriCoachException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatiServiceTest {

    @Mock
    private WatiProperties watiProperties;

    @InjectMocks
    private WatiService watiService;

    @Test
    void sendTextMessage_LocalDev_ReturnsMockId() {
        when(watiProperties.getApiToken()).thenReturn("local-token123");
        
        String id = watiService.sendTextMessage("919876543210", "Hello");
        
        assertEquals("local-mock-id", id);
    }
    
    @Test
    void sendTextMessage_ApiFailure_ThrowsException() {
        // Return a valid-looking token so it bypasses dev check
        when(watiProperties.getApiToken()).thenReturn("prod-token");
        // Return a URL that will definitely fail connection immediately
        when(watiProperties.getApiEndpoint()).thenReturn("http://localhost:1");
        
        assertThrows(NutriCoachException.class, () -> watiService.sendTextMessage("919876543210", "Hello"));
    }
}
