// Chatbot JavaScript
class Chatbot {
    constructor() {
        this.sessionId = this.getOrCreateSessionId();
        this.isOpen = false;
        this.isTyping = false;
        this.init();
    }

    init() {
        this.createChatbotUI();
        this.attachEventListeners();
        this.addWelcomeMessage();
    }

    getOrCreateSessionId() {
        let sessionId = sessionStorage.getItem('chatbot_session_id');
        if (!sessionId) {
            sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem('chatbot_session_id', sessionId);
        }
        return sessionId;
    }

    createChatbotUI() {
        const container = document.createElement('div');
        container.className = 'chatbot-container';
        container.innerHTML = `
            <button class="chatbot-toggle" id="chatbot-toggle" aria-label="Toggle chatbot">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
                </svg>
            </button>
            
            <div class="chatbot-window" id="chatbot-window">
                <div class="chatbot-header">
                    <div class="chatbot-header-content">
                        <div class="chatbot-avatar">🤖</div>
                        <div class="chatbot-title">
                            <h3>Trợ lý Du lịch</h3>
                            <p>Tư vấn tour 24/7</p>
                        </div>
                    </div>
                    <button class="chatbot-close" id="chatbot-close" aria-label="Close chatbot">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                        </svg>
                    </button>
                </div>
                
                <div class="chatbot-messages" id="chatbot-messages">
                    <!-- Messages will be inserted here -->
                </div>
                
                <div class="chatbot-input">
                    <div class="input-wrapper">
                        <input 
                            type="text" 
                            id="chatbot-message-input" 
                            placeholder="Nhập câu hỏi của bạn..."
                            autocomplete="off"
                        />
                        <button class="send-button" id="chatbot-send-button" aria-label="Send message">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(container);
    }

    attachEventListeners() {
        const toggle = document.getElementById('chatbot-toggle');
        const close = document.getElementById('chatbot-close');
        const input = document.getElementById('chatbot-message-input');
        const sendButton = document.getElementById('chatbot-send-button');

        toggle.addEventListener('click', () => this.toggleChat());
        close.addEventListener('click', () => this.closeChat());
        sendButton.addEventListener('click', () => this.sendMessage());
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !this.isTyping) {
                this.sendMessage();
            }
        });
    }

    toggleChat() {
        const window = document.getElementById('chatbot-window');
        this.isOpen = !this.isOpen;
        
        if (this.isOpen) {
            window.classList.add('active');
            document.getElementById('chatbot-message-input').focus();
        } else {
            window.classList.remove('active');
        }
    }

    closeChat() {
        const window = document.getElementById('chatbot-window');
        window.classList.remove('active');
        this.isOpen = false;
    }

    addWelcomeMessage() {
        const welcomeText = 'Xin chào! 👋 Tôi là trợ lý tư vấn du lịch. Bạn có thể hỏi tôi về các tour du lịch, địa điểm, giá cả và lịch trình. Tôi có thể giúp gì cho bạn hôm nay?';
        this.addMessage(welcomeText, 'bot');
    }

    async sendMessage() {
        const input = document.getElementById('chatbot-message-input');
        const message = input.value.trim();
        
        if (!message || this.isTyping) return;
        
        // Add user message to chat
        this.addMessage(message, 'user');
        input.value = '';
        
        // Show typing indicator
        this.showTypingIndicator();
        
        try {
            const response = await fetch('/api/chatbot/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    message: message,
                    sessionId: this.sessionId
                })
            });
            
            const data = await response.json();
            
            this.hideTypingIndicator();
            
            if (data.success) {
                this.addMessage(data.response, 'bot');
            } else {
                this.addMessage('Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.', 'bot');
            }
        } catch (error) {
            console.error('Error sending message:', error);
            this.hideTypingIndicator();
            this.addMessage('Không thể kết nối với server. Vui lòng kiểm tra kết nối internet.', 'bot');
        }
    }

    addMessage(text, sender) {
        const messagesContainer = document.getElementById('chatbot-messages');
        const messageDiv = document.createElement('div');
        messageDiv.className = `chat-message ${sender}`;
        
        const time = new Date().toLocaleTimeString('vi-VN', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
        
        const avatar = sender === 'user' ? '👤' : '🤖';
        
        messageDiv.innerHTML = `
            <div class="message-avatar">${avatar}</div>
            <div class="message-content">
                ${this.formatMessage(text)}
                <div class="message-time">${time}</div>
            </div>
        `;
        
        messagesContainer.appendChild(messageDiv);
        this.scrollToBottom();
    }

    formatMessage(text) {
        // Convert URLs to clickable links
        text = text.replace(
            /(https?:\/\/[^\s]+)/g, 
            '<a href="$1" target="_blank" rel="noopener">$1</a>'
        );
        
        // Convert line breaks to <br>
        text = text.replace(/\n/g, '<br>');
        
        return text;
    }

    showTypingIndicator() {
        this.isTyping = true;
        const messagesContainer = document.getElementById('chatbot-messages');
        const typingDiv = document.createElement('div');
        typingDiv.className = 'chat-message bot';
        typingDiv.id = 'typing-indicator';
        typingDiv.innerHTML = `
            <div class="message-avatar">🤖</div>
            <div class="message-content">
                <div class="typing-indicator active">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
            </div>
        `;
        messagesContainer.appendChild(typingDiv);
        this.scrollToBottom();
        
        document.getElementById('chatbot-send-button').disabled = true;
    }

    hideTypingIndicator() {
        this.isTyping = false;
        const typingIndicator = document.getElementById('typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
        document.getElementById('chatbot-send-button').disabled = false;
    }

    scrollToBottom() {
        const messagesContainer = document.getElementById('chatbot-messages');
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}

// Initialize chatbot when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new Chatbot();
    });
} else {
    new Chatbot();
}
