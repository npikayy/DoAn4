# 🤖 AI Chatbot với RAG - Hướng Dẫn Sử Dụng

## Tổng Quan

Chatbot AI được tích hợp vào website với các tính năng:
- ✅ **Sử dụng Google Gemini AI** - Model AI tiên tiến từ Google
- 🧠 **RAG (Retrieval-Augmented Generation)** - Tìm kiếm thông tin tour tự động
- 💬 **Lưu lịch sử chat** - Ghi nhớ cuộc hội thoại
- 🎨 **UI đẹp mắt** - Giao diện hiện đại, responsive
- ⚡ **Hash-based Embeddings** - Không cần API bên ngoài cho embeddings

## Công Nghệ Sử Dụng

### Backend
- **LLM**: Google Gemini Pro
- **Embedding**: Hash-based embeddings (384 dimensions)
- **Vector Store**: In-memory (không cần database riêng)
- **Spring Boot**: REST API

### Frontend
- **Vanilla JavaScript**: Không cần framework
- **CSS3**: Animations và modern design
- **HTML5**: Semantic markup

## Cài Đặt

### 1. Lấy Gemini API Key

1. Truy cập [Google AI Studio](https://aistudio.google.com/api-keys)
2. Đăng nhập với Google account
3. Click "Create API Key"
4. Copy API key

### 2. Cấu hình trong `application.yaml`

```yaml
gemini:
  api-key: "YOUR_API_KEY_HERE"  # Thay bằng API key của bạn
  model: "gemini-pro"
  base-url: "https://generativelanguage.googleapis.com/v1beta/models"

ai:
  chatbot:
    enabled: true
    max-tokens: 250
    rag:
      top-k: 3
      similarity-threshold: 0.7
    embedding:
      dimension: 384
```

### 3. Build và chạy

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

## Cách Hoạt Động

### 1. Khởi tạo Vector Store

Khi application start:
- Tất cả tours trong database được đọc
- Mỗi tour được chuyển thành text description
- Text được chuyển thành vector embedding (384 dimensions) bằng hash-based method
- Lưu vào in-memory vector store

### 2. Xử lý câu hỏi người dùng

```
User Question
     ↓
Convert to Embedding (Hash-based)
     ↓
Search Similar Tours (Cosine Similarity)
     ↓
Get Top 3 Relevant Tours
     ↓
Build Context + Prompt
     ↓
Call Gemini API
     ↓
Generate Response
     ↓
Save to Database
```

### 3. RAG Pipeline

1. **Retrieval**: Tìm 3 tours liên quan nhất (similarity >= 0.7)
2. **Augmentation**: Thêm thông tin tours vào prompt
3. **Generation**: Gemini AI tạo câu trả lời dựa trên context

## API Endpoints

### Chat với Bot
```http
POST /api/chatbot/chat
Content-Type: application/json

{
  "message": "Tôi muốn đi Đà Nẵng",
  "sessionId": "session_123" // optional
}
```

Response:
```json
{
  "response": "Chào bạn! Đà Nẵng có nhiều tour tuyệt vời...",
  "sessionId": "session_123",
  "success": true,
  "error": null
}
```

### Lấy lịch sử chat
```http
GET /api/chatbot/history/{sessionId}
```

## Sử Dụng Trên Frontend

Chatbot tự động xuất hiện ở góc dưới bên phải mọi trang client:

1. **Click nút chat** (icon tin nhắn màu tím)
2. **Nhập câu hỏi** về tour du lịch
3. **Nhận phản hồi** từ AI với thông tin tour phù hợp

### Ví dụ câu hỏi:
- "Tôi muốn đi du lịch Đà Nẵng"
- "Tour nào giá rẻ nhất?"
- "Có tour nào đi Phú Quốc không?"
- "Tour miền Bắc có gì hay?"

## Tính Năng Nâng Cao

### 1. Context-Aware Responses
Bot nhớ 2 tin nhắn gần nhất trong conversation để trả lời phù hợp context.

### 2. Fallback Handling
Nếu API bị lỗi hoặc không có tours phù hợp, bot vẫn trả lời lịch sự.

### 3. Hash-based Embeddings
Sử dụng thuật toán hash thông minh để tạo embeddings:
- Không cần gọi API bên ngoài
- Rất nhanh (< 50ms)
- Deterministic - cùng input luôn cho cùng output
- Bảo toàn một phần semantic similarity

## Tùy Chỉnh

### Thay đổi số lượng tours được retrieve
```yaml
ai:
  chatbot:
    rag:
      top-k: 5  # Tăng lên 5 tours
```

### Thay đổi ngưỡng similarity
```yaml
ai:
  chatbot:
    rag:
      similarity-threshold: 0.6  # Giảm xuống để lấy nhiều kết quả hơn
```

### Thay đổi Gemini model
```yaml
gemini:
  model: "gemini-1.5-pro"  # Model mới hơn, mạnh hơn
```

### Điều chỉnh response length
```yaml
ai:
  chatbot:
    max-tokens: 500  # Tăng để có câu trả lời dài hơn
```

## Xử Lý Lỗi

### Gemini API Error
**Nguyên nhân:** API key không hợp lệ hoặc hết quota

**Giải pháp:**
1. Kiểm tra API key trong application.yaml
2. Xác nhận API key còn hoạt động tại [Google AI Studio](https://aistudio.google.com)
3. Kiểm tra quota tại [Google Cloud Console](https://console.cloud.google.com)

### Database connection
Nếu không kết nối được database:
- Vector store sẽ empty
- Bot vẫn hoạt động nhưng không có context về tours

## Performance

- **Vector Store Init**: ~0.5-1 giây (hash-based embeddings rất nhanh)
- **Embedding Generation**: ~50ms per query
- **Gemini Response**: ~2-5 giây
- **Total Response Time**: ~2-6 giây

## Bảo Trì

### Refresh Vector Store
Khi thêm tours mới, cần restart application hoặc call:
```java
vectorStoreService.refreshVectorStore();
```

### Clear Chat History
```sql
DELETE FROM chat_messages WHERE created_at < NOW() - INTERVAL '30 days';
```

## Troubleshooting

### Chatbot không xuất hiện
- Kiểm tra file `/css/chatbot.css` và `/js/chatbot.js` đã load
- Xem console browser có lỗi JavaScript

### API trả về lỗi
- Kiểm tra `ai.chatbot.enabled=true` trong application.yaml
- Xem logs Spring Boot để debug
- Verify Gemini API key còn hoạt động

### Response chậm
- Gemini API có thể chậm trong lần đầu
- Kiểm tra kết nối internet

## Ưu Điểm của Hash-based Embeddings

| Feature | Hash-based Embeddings |
|---------|----------------------|
| **Setup** | Không cần gì |
| **Cost** | Hoàn toàn miễn phí |
| **Speed** | Rất nhanh (< 50ms) |
| **Internet** | Không cần cho embeddings |
| **Rate Limits** | Không có |
| **Accuracy** | Tốt cho keyword matching |
| **Deterministic** | Luôn cho kết quả giống nhau |

## Giới Hạn

1. **Gemini API Quota**: Free tier có giới hạn requests
2. **Context window**: Chỉ nhớ 2 tin nhắn gần nhất
3. **Language**: Tối ưu cho tiếng Việt
4. **Hash-based embeddings**: Semantic similarity không hoàn hảo như neural embeddings

## Tương Lai

- [ ] Add voice input/output
- [ ] Multi-language support
- [ ] Integration với booking system
- [ ] Analytics dashboard
- [ ] Gemini 1.5 Pro integration

## Liên Hệ

Nếu có vấn đề, liên hệ qua:
- GitHub Issues
- Email: support@toididulich.com

---

**Powered by Google Gemini AI** 🚀
