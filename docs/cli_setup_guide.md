# CLI Tool Setup Guide for AI Code Generation Case Study

This guide explains how to set up CLI tools for the AI code generation case study.

## Current Implementation

The generator supports multiple CLI tools with automatic fallback to mock mode:

### Windows Support
- **Primary**: Windsurf CLI (`windsurf.cmd chat`)
- **Fallback**: Mock mode with realistic code generation

### Linux Support  
- **Primary**: OpenCode CLI (`opencode run`)
- **Fallback**: Mock mode with realistic code generation

## Setup Requirements

### Option 1: Install OpenAI CLI (Recommended for Windows)
```bash
# Install OpenAI CLI
npm install -g @openai/opeai

# Set up API key
export OPENAI_API_KEY="your-api-key-here"

# Test
openai api chat.create -m "Generate a simple Java class"
```

### Option 2: Install Anthropic CLI (Alternative)
```bash
# Install Anthropic CLI
npm install -g @anthropic-ai/cli

# Set up API key  
export ANTHROPIC_API_KEY="your-api-key-here"

# Test
anthropic messages create -m "Generate a simple Java class"
```

### Option 3: Configure Windsurf for Programmatic Use
Windsurf is primarily a GUI editor, but you can try:

```bash
# Try different modes
windsurf.cmd chat -m ask "your prompt"
windsurf.cmd chat -m edit "your prompt" 
windsurf.cmd chat -m agent "your prompt"
```

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
   ```bash
   # Windows
   windsurf.cmd --help
   
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
