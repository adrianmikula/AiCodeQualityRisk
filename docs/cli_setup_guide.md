# CLI Tool Setup Guide for AI Code Generation Case Study

This guide explains how to set up CLI tools for the AI code generation case study.

## Current Implementation

The generator supports multiple CLI tools with automatic fallback to mock mode:

### Windows Support
- **Primary**: aichat via nvm-windows (multiple LLM providers)
- **Secondary**: Ollama (local models, completely free)
- **Fallback**: Mock mode with realistic code generation

### Linux Support  
- **Primary**: OpenCode CLI (`opencode run`)
- **Fallback**: Mock mode with realistic code generation

## Setup Requirements

**Two fully-supported approaches:**

1. **NVM-Windows + aichat** (Recommended) - Node.js-based, multiple LLM providers
2. **Ollama** (Local) - Completely free, runs locally, no API keys

Choose based on your preference:
- **Want multiple LLM options?** → Use Approach 1 (nvm-windows + aichat)
- **Want completely local/ free?** → Use Approach 2 (ollama)

---

### 🎯 Supported CLI Tools for Windows

### 1. **aichat** (Recommended)

#### Setup via NVM-Windows
The cleanest way to install aichat on Windows with proper PATH management.

```powershell
# 1. Install nvm-windows
winget install CoreyButler.NVMforWindows

# 2. IMPORTANT: Close and reopen PowerShell to reload PATH
#    (nvm-windows changes system PATH, needs new shell session)

# 3. Verify nvm works in new session
nvm version

# 4. Install Node.js LTS
nvm install 20.11.0
nvm use 20.11.0

# 5. Install aichat (PATH works automatically with nvm)
npm install -g aichat

# 6. Verify (if this fails, restart PowerShell again)
aichat --version
```

**⚠️ Critical:** Always restart PowerShell after installing nvm-windows so PATH changes take effect.

```powershell
# 7. Configure for free providers
aichat --set-provider ollama  # For local models
# OR
aichat --set-provider groq    # For free API

# 8. Test
aichat "Generate a simple Java class"
```

**Why NVM-Windows:**
- ✅ Clean PATH management (no manual editing)
- ✅ Global packages work immediately
- ✅ No environment variable issues
- ✅ Easy Node.js version switching

**Why it's great:**
- ✅ Windows binaries available
- ✅ Supports 20+ LLM providers
- ✅ Works with free APIs (Groq, Ollama, OpenRouter)
- ✅ Unified interface across providers
- ✅ No vendor lock-in
- ✅ **Already integrated in LlmCaller**

### 2. **Ollama** (Local Models - Approach 2)

#### Windows Setup
```powershell
# Download and install Ollama for Windows
# Option 1: Download installer from https://ollama.com/download
# Option 2: Use winget (Windows Package Manager)
winget install Ollama.Ollama

# Verify installation
ollama --version

# Pull free models (choose based on your hardware)
ollama pull llama2          # General purpose
ollama pull codellama       # Code-focused
ollama pull mistral         # Efficient and capable
ollama pull llama2:7b       # Smaller, faster model

# Test with a simple prompt
ollama run llama2 "Generate a simple Java Spring Boot REST API with CRUD operations"
```

**Prerequisites:**
- Windows 10/11 with WSL2 or native Windows support
- Sufficient RAM (8GB+ recommended, 16GB+ for larger models)
- GPU optional but recommended for faster inference

**Hardware Requirements:**
- **7B models**: 8GB RAM minimum
- **13B models**: 16GB RAM minimum  
- **33B+ models**: 32GB+ RAM recommended

**Why it's perfect:**
- ✅ Completely free (runs locally)
- ✅ No API keys needed
- ✅ Multiple code-focused models available
- ✅ Windows native support
- ✅ **Already integrated in LlmCaller**

## 🔧 Generator Integration Status

**Recommended setup priority:**

1. **NVM-Windows + aichat** (Best for most users)
2. **Ollama** (Best for completely free/local execution)

**Automatic Detection Order:**
```
aichat → ollama → mock mode
```

**Features:**
- ✅ **Tries multiple tools** automatically if one fails
- ✅ **Fallback to mock mode** if no CLI tools available
- ✅ **Progress feedback** shows which tool is being tried
- ✅ **Cross-platform**: Windows (nvm-based tools, ollama) and Linux (opencode)

## Configuration Updates Needed

### Update generator.json
```json
{
  "model": "gpt-4",  // or "claude-3.5-sonnet"
  "maxRetries": 3,
  "retryDelayMs": 2000
}
```

### Update LlmCaller.kt if needed
The current implementation automatically detects CLI tools and falls back to mock mode. No code changes needed unless you want to:

1. **Add new CLI tools**: Add new `call<ToolName>Windows()` or `call<ToolName>Unix()` methods
2. **Change model parameters**: Update default model in constructor
3. **Adjust retry logic**: Modify `maxRetries` and `retryDelayMs`

## Testing Setup

1. **Test CLI tool availability**:
   ```powershell
   # Windows - Test aichat
   aichat --version
   
   # Windows - Test Ollama (alternative)
   ollama --version
   
   # Linux  
   opencode --help
   ```

2. **Test API connectivity**:
   ```bash
   # Test with simple prompt
   .\gradlew.bat run --args="config/test-generator.json"
   ```

3. **Verify output format**:
   - Should return XML-wrapped Java files
   - Should be parsable by FileExtractor
   - Should generate realistic code with duplication patterns

## Mock Mode Features

The enhanced mock mode generates varied, realistic code patterns:

- **Basic CRUD**: Standard Spring Boot CRUD operations
- **Iterative**: Code with intentional duplication patterns
- **Authentication**: JWT tokens, security filters
- **Validation**: DTOs with validation annotations
- **Caching**: Redis caching services

This allows the case study to demonstrate:
- Code duplication patterns in AI-generated code
- Differences between single-shot vs iterative generation
- Realistic code quality metrics

## Next Steps

1. Install your preferred CLI tool
2. Configure API keys if needed
3. Test with the provided test configuration
4. Run full experiment when ready

The generator will automatically use available CLI tools and provide meaningful results for the case study.
