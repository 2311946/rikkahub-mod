# RikkaHub-Mod Project Guide

## Project Overview

RikkaHub is an Android AI chat client supporting multiple LLM providers (OpenAI, Google, Claude), with features including MCP tool integration, web search, workspace (code execution), voice mode, image/video generation, and more. Built with Kotlin, Jetpack Compose, Koin DI, Room database.

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew test                    # Run unit tests
./gradlew :app:testDebugUnitTest  # App module tests only
```

## Multi-Module Architecture

```
rikkahub/
  app/          Main Android app (UI, DI, service, data layer)
  ai/           Core AI abstractions (Provider, Model, UIMessage, streaming)
  search/       Web search service implementations (20+ providers)
  speech/       ASR/TTS provider integrations
  workspace/    Sandboxed shell/file execution environment
  document/     Document parsing (PDF, DOCX, etc.)
  highlight/    Syntax highlighting for code blocks
  common/       Shared utilities
  material3/    Custom Material3 components
  oauth/        OAuth2 client for MCP servers
  videogen/     Video generation provider
  web/          Ktor-based embedded web server
  web-ui/       Frontend for web server (JS/Bun)
  locale-tui/   CLI localization tool
  trace-cli/    Stream trace replay CLI
  build-logic/  Gradle convention plugins
```

## Core File Groups (ABCD Priority)

### Group A: Critical Path (must understand to make any chat feature change)

| File | Role |
|------|------|
| `ai/.../ui/UIMessagePart.kt` | All message part types: Text, Image, Tool, ServerTool, Reasoning, etc. |
| `ai/.../ui/Message.kt` | UIMessage data class: the universal message unit |
| `ai/.../provider/Provider.kt` | Provider interface: generateText/streamText contract |
| `ai/.../provider/ProviderManager.kt` | Registry of OpenAI/Google/Claude provider impls |
| `ai/.../provider/ProviderSetting.kt` | Sealed class: OpenAI, Google, Claude connection settings |
| `ai/.../provider/Model.kt` | Model data class with abilities, tools, custom headers |
| `ai/.../core/Tool.kt` | Tool definition (name, description, parameters, execute lambda) |
| `app/.../service/ChatService.kt` | Heart of the app (1426 lines): session mgmt, sendMessage, generation orchestration, tool approval |
| `app/.../service/ConversationSession.kt` | Per-conversation state holder with ref-counting, generation job, message queue |
| `app/.../service/MessageQueue.kt` | FIFO queue for user messages with pause/resume, voice reply deferreds |
| `app/.../data/ai/GenerationLoop.kt` | Multi-step tool-use loop: generate, check tools, approve/execute, repeat |
| `app/.../data/ai/tools/ChatToolFactory.kt` | Assembles complete tool set per generation (memory + search + local + MCP + workspace + skills) |
| `app/.../data/model/Conversation.kt` | Conversation with tree-structured MessageNodes (branching support) |
| `app/.../data/model/Assistant.kt` | Assistant config: system prompt, model, tools, memory, search, workspace, skills |

### Group B: Feature Modules (understand when touching specific features)

| File | Role |
|------|------|
| `app/.../data/ai/tools/SearchTools.kt` | search_web + scrape_web tool definitions using pluggable search services |
| `app/.../data/ai/tools/MemoryTools.kt` | memory_tool (create/edit/delete) for cross-conversation memory |
| `app/.../data/ai/tools/WorkspaceTools.kt` | workspace_read_file, workspace_write_file, workspace_edit_file, workspace_shell |
| `app/.../data/ai/tools/SkillsTools.kt` | Skill (prompt template) tool execution |
| `app/.../data/ai/tools/ConversationTools.kt` | Recent chat history reference tool |
| `app/.../data/ai/tools/local/*.kt` | 7 local tools: JavaScript, TimeInfo, Clipboard, TTS, AskUser, ScreenTime, Calendar |
| `app/.../data/ai/mcp/McpManager.kt` | MCP server lifecycle, tool discovery, session management |
| `app/.../data/ai/mcp/McpSessionRegistry.kt` | Active MCP session tracking |
| `app/.../data/ai/mcp/McpConfig.kt` | MCP server configuration model |
| `app/.../data/ai/transformers/*.kt` | 10 message transformers: time reminder, placeholder, OCR, template, think-tag, etc. |
| `search/*.kt` | 20+ search service implementations (Tavily, Brave, Jina, SearXNG, Bing, etc.) |
| `ai/.../providers/openai/` | OpenAI provider: ChatCompletions API + Response API + streaming |
| `ai/.../providers/google/` | Google/Gemini provider with grounding/search support |
| `ai/.../providers/claude/` | Claude/Anthropic provider with server tools |
| `ai/.../ui/StreamChunkHandler.kt` | Maps raw StreamChunk events to UIMessage updates |

### Group C: UI Layer (understand for visual/UX changes)

| File | Role |
|------|------|
| `app/.../ui/pages/chat/ChatPage.kt` | Main chat screen composable |
| `app/.../ui/pages/chat/ChatVM.kt` | Chat ViewModel: bridges UI to ChatService |
| `app/.../ui/pages/chat/ChatList.kt` | Message list rendering |
| `app/.../ui/pages/chat/VoiceMode.kt` | Voice conversation UI |
| `app/.../ui/pages/chat/VoiceSessionController.kt` | Voice session state machine |
| `app/.../ui/components/ai/ChatInput.kt` | Message input bar with attachments, model picker |
| `app/.../ui/components/message/ChatMessage.kt` | Single message bubble rendering |
| `app/.../ui/components/message/ChatMessageTools.kt` | Tool call result display |
| `app/.../ui/components/message/tools/ToolUI.kt` | Tool-specific UI renderers |
| `app/.../ui/components/richtext/Markdown.kt` | Markdown rendering |
| `app/.../ui/pages/setting/SettingPage.kt` | Main settings screen |
| `app/.../ui/pages/setting/SettingProviderPage.kt` | Provider configuration UI |
| `app/.../ui/pages/setting/SettingMcpPage.kt` | MCP server management UI |
| `app/.../ui/pages/assistant/` | Assistant create/edit pages |
| `app/.../ui/pages/extensions/workspace/` | Workspace file browser, terminal, editor |

### Group D: Infrastructure (rarely need to touch directly)

| File | Role |
|------|------|
| `app/.../di/*.kt` | Koin DI modules (AppModule, DataSourceModule, RepositoryModule, ViewModelModule) |
| `app/.../data/db/` | Room database, DAOs, entities, migrations |
| `app/.../data/repository/` | Repository layer (Conversation, Favorite, Files, Folder, Memory, Workspace) |
| `app/.../data/datastore/PreferencesStore.kt` | DataStore-based settings persistence |
| `app/.../data/sync/` | Backup/restore (WebDAV, S3, import/export) |
| `app/.../web/` | Embedded Ktor web server + REST API |
| `app/.../utils/*.kt` | 20+ utility files (JSON, image, clipboard, markdown, etc.) |

## ChatService Message Flow

```
User types message
       |
       v
ChatService.sendMessage(conversationId, parts)
       |  enqueue to MessageQueue
       v
dispatchNextQueuedMessage()
       |  dequeue, check no pending tool approvals
       v
sendQueuedMessage(session, queued)
       |  1. Resolve assistant, model, provider from settings
       |  2. preprocessUserInputParts() for file handling
       |  3. Append user message to conversation
       |  4. ChatToolFactory.createTools() assembles tool set:
       |     memory + search + local + conversation + workspace + skills + MCP
       |  5. Launch GenerationLoop.generateText()
       v
GenerationLoop (multi-step loop, up to 256 steps):
       |  For each step:
       |  - Apply InputTransformers (time, placeholder, OCR, etc.)
       |  - Call Provider.streamText() or generateText()
       |  - StreamChunkHandler maps chunks to UIMessage updates
       |  - Apply OutputTransformers (think-tag, base64-to-file, regex)
       |  - Check for tool calls in response
       |    - needsApproval? set ToolApprovalState.Pending, break
       |    - auto-approved? execute tool, append result, continue loop
       |  - No tool calls? break (generation complete)
       v
handleMessageComplete()
       |  1. Save conversation to database
       |  2. Auto-generate title if needed
       |  3. Emit generationDoneFlow
       |  4. Dispatch next queued message
```

## Tool Approval Flow

```
Tool call received from LLM
       |
       +-- Tool.needsApproval(input) == false --> Auto (execute immediately)
       |
       +-- Tool.needsApproval(input) == true --> Pending
            |  UI shows approval dialog
            +-- User approves --> Approved --> resume GenerationLoop
            +-- User denies --> Denied(reason) --> send denial as tool result
            +-- User answers (AskUser) --> Answered(text) --> send as tool result
```

## Existing Features (comprehensive)

- Multi-provider support (OpenAI, Google/Gemini, Claude)
- OpenAI ChatCompletions API + Response API
- Google native search grounding
- Claude server-side tools (web_search, code_execution)
- External web search via 20+ search services as tool
- MCP server integration (stdio/SSE/streamable-http with OAuth)
- Local tools: JavaScript, TimeInfo, Clipboard, TTS, AskUser, ScreenTime, Calendar
- Workspace tools: read/write/edit file, shell execution
- Memory tools: create/edit/delete cross-conversation memory
- Skills tools: prompt template execution
- Tool approval/denial flow with user interaction
- Multi-step tool-use loop (up to 256 iterations)
- Message branching (tree-structured conversation)
- Voice mode (ASR + TTS)
- Image generation
- Video generation
- Document parsing (PDF, DOCX)
- Chat export (text, image, markdown)
- Backup/restore (WebDAV, S3, local)
- Embedded web server (REST API for external access)
- Streaming output with reasoning/thinking display
- Context compression
- Translation of messages
- 10 message transformers pipeline
- Conversation system prompt per-conversation override
- Mode injections and lorebooks
- Group chat (multi-assistant)
- Full-text search across messages

## Architecture Patterns

- DI: Koin (AppModule, DataSourceModule, RepositoryModule, ViewModelModule)
- State: StateFlow + MutableStateFlow throughout
- DB: Room with typed JSON columns for messages
- Navigation: Jetpack Compose Navigation
- Serialization: kotlinx.serialization
- HTTP: OkHttp (AI providers) + Ktor (embedded server)
- Concurrency: Kotlin coroutines + Flow
- Message model: UIMessage with List of UIMessagePart (sealed class hierarchy)
- Provider model: Stateless Provider<T> with ProviderSetting for config
- Tool model: Tool with name/description/parameters/execute lambda + needsApproval
- Transformer pipeline: InputMessageTransformer and OutputMessageTransformer applied before/after generation
