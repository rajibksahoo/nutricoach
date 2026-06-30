package com.nutricoach.billing.service;

import com.nutricoach.billing.config.RazorpayProperties;
import com.nutricoach.billing.entity.Subscription;
import com.nutricoach.coach.entity.Coach;
import com.razorpay.Customer;
import com.razorpay.CustomerClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.SubscriptionClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private RazorpayProperties props;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private SubscriptionClient subscriptionClient;

    @InjectMocks
    private RazorpayService razorpayService;

    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field customersField = RazorpayClient.class.getDeclaredField("customers");
        customersField.setAccessible(true);
        customersField.set(razorpayClient, customerClient);

        java.lang.reflect.Field subsField = RazorpayClient.class.getDeclaredField("subscriptions");
        subsField.setAccessible(true);
        subsField.set(razorpayClient, subscriptionClient);
    }
    
    private void setId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field idField = com.nutricoach.common.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void ensureCustomer_DevMode_ReturnsDummy() {
        when(props.isDevMode()).thenReturn(true);
        Coach coach = new Coach();
        setId(coach, UUID.randomUUID());
        
        String custId = razorpayService.ensureCustomer(coach);
        
        assertTrue(custId.startsWith("cust_dev_"));
        verifyNoInteractions(razorpayClient);
    }
    
    @Test
    void ensureCustomer_AlreadyExists_ReturnsExistingId() {
        when(props.isDevMode()).thenReturn(false);
        Coach coach = new Coach();
        coach.setRazorpayCustomerId("cust_12345");
        
        String custId = razorpayService.ensureCustomer(coach);
        
        assertEquals("cust_12345", custId);
        verifyNoInteractions(razorpayClient);
    }
    
    @Test
    void ensureCustomer_ApiSuccess_ReturnsNewId() throws Exception {
        when(props.isDevMode()).thenReturn(false);
        Coach coach = new Coach();
        setId(coach, UUID.randomUUID());
        coach.setName("Test Coach");
        coach.setPhone("9876543210");
        coach.setEmail("test@coach.com");
        
        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.get("id")).thenReturn("cust_new123");
        when(customerClient.create(any(JSONObject.class))).thenReturn(mockCustomer);
        
        String custId = razorpayService.ensureCustomer(coach);
        
        assertEquals("cust_new123", custId);
        verify(customerClient).create(any(JSONObject.class));
    }
    
    @Test
    void ensureCustomer_ApiFailure_ThrowsException() throws Exception {
        when(props.isDevMode()).thenReturn(false);
        Coach coach = new Coach();
        setId(coach, UUID.randomUUID());
        
        when(customerClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("API Error"));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> razorpayService.ensureCustomer(coach));
        assertTrue(exception.getMessage().contains("Failed to create payment customer"));
    }
    
    @Test
    void createSubscription_DevMode_ReturnsDummy() {
        when(props.isDevMode()).thenReturn(true);
        
        String[] result = razorpayService.createSubscription("cust_123", Subscription.PlanTier.PROFESSIONAL);
        
        assertTrue(result[0].startsWith("sub_dev_professional_"));
        assertTrue(result[1].startsWith("https://rzp.io/dev/"));
        verifyNoInteractions(razorpayClient);
    }
    
    @Test
    void createSubscription_ApiSuccess_ReturnsIdAndUrl() throws Exception {
        when(props.isDevMode()).thenReturn(false);
        when(props.planIdFor("PROFESSIONAL")).thenReturn("plan_pro_123");
        
        com.razorpay.Subscription mockSub = mock(com.razorpay.Subscription.class);
        when(mockSub.get("id")).thenReturn("sub_new123");
        when(mockSub.get("short_url")).thenReturn("https://rzp.io/short");
        when(subscriptionClient.create(any(JSONObject.class))).thenReturn(mockSub);
        
        String[] result = razorpayService.createSubscription("cust_123", Subscription.PlanTier.PROFESSIONAL);
        
        assertEquals("sub_new123", result[0]);
        assertEquals("https://rzp.io/short", result[1]);
        verify(subscriptionClient).create(any(JSONObject.class));
    }
    
    @Test
    void createSubscription_ApiFailure_ThrowsException() throws Exception {
        when(props.isDevMode()).thenReturn(false);
        when(props.planIdFor("PROFESSIONAL")).thenReturn("plan_pro_123");
        
        when(subscriptionClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("API Error"));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> razorpayService.createSubscription("cust_123", Subscription.PlanTier.PROFESSIONAL));
        assertTrue(exception.getMessage().contains("Failed to create subscription"));
    }
    
    @Test
    void cancelSubscription_DevMode_NoOp() {
        when(props.isDevMode()).thenReturn(true);
        
        assertDoesNotThrow(() -> razorpayService.cancelSubscription("sub_123"));
        verifyNoInteractions(razorpayClient);
    }
    
    @Test
    void cancelSubscription_ApiSuccess_CallsCancel() throws Exception {
        when(props.isDevMode()).thenReturn(false);
        when(subscriptionClient.cancel(eq("sub_123"), any(JSONObject.class))).thenReturn(null);
        
        assertDoesNotThrow(() -> razorpayService.cancelSubscription("sub_123"));
        verify(subscriptionClient).cancel(eq("sub_123"), any(JSONObject.class));
    }
    
    @Test
    void cancelSubscription_ApiFailure_ThrowsException() throws Exception {
        when(props.isDevMode()).thenReturn(false);
        
        when(subscriptionClient.cancel(eq("sub_123"), any(JSONObject.class))).thenThrow(new RazorpayException("API Error"));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> razorpayService.cancelSubscription("sub_123"));
        assertTrue(exception.getMessage().contains("Failed to cancel subscription"));
    }

    @Test
    void verifyWebhookSignature_DevMode_ReturnsTrue() {
        when(props.isDevMode()).thenReturn(true);
        assertTrue(razorpayService.verifyWebhookSignature("payload", "signature"));
    }
}
