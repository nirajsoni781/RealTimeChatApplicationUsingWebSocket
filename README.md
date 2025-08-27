1. Class-Level Annotations 

@Configuration 
@EnableWebSocketMessageBroker 
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer { 
 

@Configuration: Marks this class as a configuration component for Spring (like a Java-based applicationContext.xml). 

@EnableWebSocketMessageBroker: Turns on WebSocket/STOMP message handling in Spring. It: 

Registers STOMP endpoints. 

Enables Spring’s SimpMessagingTemplate and message broker support. 

WebSocketMessageBrokerConfigurer: Interface to customize STOMP/WebSocket behavior (endpoint registration, broker config). 

 

2. Registering the STOMP Endpoint 

@Override 
public void registerStompEndpoints(StompEndpointRegistry registry) { 
    registry.addEndpoint("/ws") 
            .addInterceptors(new UserHandshakeInterceptor()) 
            .setHandshakeHandler(new DefaultHandshakeHandler() { 
                @Override 
                protected Principal determineUser( 
                        ServerHttpRequest request,    
                        WebSocketHandler wsHandler, 
                        Map<String, Object> attributes) { 
                    String username = (String) attributes.get("user"); 
                    return () -> username; // return Principal with that name 
                } 
            }) 
            .setAllowedOriginPatterns("*") 
            .withSockJS(); 
} 
 

What it does: 

/ws: Creates a WebSocket/STOMP endpoint at /ws. Clients connect to this URL. 

addInterceptors(new UserHandshakeInterceptor()): 

Adds a custom handshake interceptor (likely sets attributes like user into the session). 

Runs before the connection is established. 

setHandshakeHandler(...): 

Customizes how Spring resolves the Principal (the authenticated user). 

Here it reads attributes.get("user") and returns a Principal with that name. 
 Example: if handshake attributes contain user=neeraj, then Principal.getName() returns "neeraj". 

setAllowedOriginPatterns("*"): 

Allows connections from any domain (CORS for WebSocket). 

withSockJS(): 

Enables SockJS fallback (if a browser does not support native WebSockets). 

Client libraries can connect using long polling or other techniques. 

 

3. Configuring the Message Broker 

@Override 
public void configureMessageBroker(MessageBrokerRegistry registry) { 
    registry.setApplicationDestinationPrefixes("/app"); 
    registry.enableSimpleBroker("/topic", "/queue"); 
    registry.setUserDestinationPrefix("/user"); 
} 
 

setApplicationDestinationPrefixes("/app"): 

All messages sent by clients to the server must start with /app. 

Example: client sends to /app/chat → handled by a @MessageMapping("/chat") method on the server. 

enableSimpleBroker("/topic", "/queue"): 

Enables an in-memory message broker for broadcasting messages to subscribers. 

/topic is typically used for public broadcasts (multiple subscribers). 

/queue is typically used for point-to-point messaging (one-to-one). 

setUserDestinationPrefix("/user"): 

Supports private, user-specific queues. 

Example: convertAndSendToUser("john", "/queue/messages", payload) sends to /user/john/queue/messages. 

 

How It Fits in a Chat Application 

Handshake: When a user connects to /ws, the UserHandshakeInterceptor extracts some user identifier (e.g., from query params, session, or token) and stores it in attributes. 

Principal Mapping: The custom HandshakeHandler converts that attribute into a Principal. 

Messaging Flow: 

Client → Server: Clients send messages to /app/.... Spring controllers listen with @MessageMapping. 

Server → Client: Server broadcasts messages using SimpMessagingTemplate to /topic/... (all) or /user/{username}/queue/... (private). 

SockJS: Makes it compatible with browsers that do not support WebSockets. 

 

Next Step 

You’ll also need: 

UserHandshakeInterceptor class to put the user info in attributes. 

A @Controller with @MessageMapping methods to handle incoming messages. 

A JavaScript/HTML client to connect and send/receive messages. 

 

 

1. Structure and Styling 

HTML Layout: 

Header: Displays your username (me) and the peer (other). 

Status Bar: Shows the connection status. 

Chat Log (#log): Displays the conversation. 

Footer: Input field and Send button. 

CSS: 

Minimal styling for readability: bubbles for messages, light colors, some padding, and rounded corners. 

.msg.me styles your own messages differently (blue-tinted bubble). 

 

2. Script Includes 

<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script> 
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script> 
 

SockJS: Provides WebSocket fallback support. 

STOMP.js: Messaging protocol over WebSocket to send/receive messages. 

 

3. Query Parameters for Users 

const params = new URLSearchParams(location.search); 
const me = params.get('user') || 'user1'; 
const other = me === 'user1' ? 'user2' : 'user1'; 
 

Reads ?user=user1 or ?user=user2 from the URL to identify the logged-in user. 

Determines the chatting peer. 

 

4. Connecting to the WebSocket Server 

const socket = new SockJS('/ws?user=' + me);  
stompClient = Stomp.over(socket); 
stompClient.debug = null; 
stompClient.connect({}, function(frame) { 
    setStatus('Connected'); 
    stompClient.subscribe('/user/queue/messages', function(message) { 
        const body = JSON.parse(message.body); 
        addMessage(body, false); 
    }); 
}); 
 

Connects to the Spring WebSocket endpoint /ws. 

Sends the username as a query parameter for identification. 

When connected, subscribes to /user/queue/messages — this is the personal message queue for this user. 

Incoming messages trigger addMessage() to display them. 

 

5. Sending Messages 

function sendMessage() { 
  const content = document.getElementById('text').value.trim(); 
  if (!content || !stompClient || !stompClient.connected) return; 
 
  const msg = { 
    sender: me, 
    receiver: other, 
    content, 
    timestamp: Date.now() 
  }; 
  stompClient.send('/app/chat.send', {}, JSON.stringify(msg)); 
  addMessage(msg, true); 
} 
 

Prepares a message object with sender, receiver, content, and timestamp. 

Sends it to the server using /app/chat.send (the controller endpoint on the backend). 

Immediately adds the message to the chat log (mine = true). 

 

6. Receiving Messages 

stompClient.subscribe('/user/queue/messages', function(message) { 
    const body = JSON.parse(message.body); 
    addMessage(body, false); 
}); 
 

Listens to the user-specific queue. 

Each received message is displayed with the sender’s name. 

 

7. Utility Functions 

setStatus(text): Updates connection status. 

addMessage(msg, mine): Creates a message row, formats timestamp, escapes HTML, scrolls log to bottom. 

escapeHtml(str): Prevents HTML injection by escaping special characters. 

Key events: hitting Enter or clicking Send triggers sendMessage(). 

 

8. Flow of the Application 

Page loads → Reads user from URL. 

Connects to WebSocket endpoint /ws. 

Subscribes to its private /user/queue/messages. 

User types a message → Click Send or hit Enter → Message goes to /app/chat.send. 

Server routes the message to the receiver’s /user/queue/messages. 

Receiver’s page receives and displays the message. 

 
