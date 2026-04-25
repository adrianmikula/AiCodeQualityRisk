package com.aicodequalityrisk.generator.llm

import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class LlmCaller(
    private val model: String = "claude-3.5-sonnet",
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 3000
) {
    fun generate(prompt: String): String {
        repeat(maxRetries) { attempt ->
            try {
                System.err.println("Calling LLM...")
                val result = callOpenCode(prompt)
                if (result.isNotBlank() && result.length > 50) {
                    System.err.println("Success: ${result.length} chars")
                    return result
                }
                System.err.println("Empty response")
            } catch (e: Exception) {
                System.err.println("Failed: ${e.message}")
            }
            Thread.sleep(retryDelayMs)
        }
        throw IllegalStateException("All attempts failed")
    }

    private fun callOpenCode(prompt: String): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        
        return try {
            if (isWindows) {
                // Try different CLI tools in order of preference
                callAichatWindows(prompt) 
                    ?: callLlmWindows(prompt)
                    ?: callOllamaWindows(prompt)
                    ?: callWindsurfWindows(prompt)
                    ?: throw IllegalStateException("All CLI tools failed")
            } else {
                callOpenCodeUnix(prompt)
            }
        } catch (e: Exception) {
            System.err.println("CLI tool not available, using mock mode: ${e.message}")
            callMockMode(prompt)
        }
    }
    
    private fun callAichatWindows(prompt: String): String? {
        val tempDir = System.getProperty("java.io.tmpdir")
        val promptFile = File.createTempFile("prompt_", ".txt", File(tempDir))
        val script = File.createTempFile("call_aichat_", ".bat", File(tempDir))
        
        try {
            promptFile.writeText(prompt)
            
            script.writeText("""
                @echo off
                cd /d "$tempDir"
                aichat @"${promptFile.name}"
            """.trimIndent())
            
            val pb = ProcessBuilder()
            pb.command("cmd", "/c", script.absolutePath)
            pb.directory(File(tempDir))
            pb.redirectErrorStream(true)
            
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            
            if (exitCode == 0 && output.isNotBlank() && output.length > 50) {
                return output
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            promptFile.delete()
            script.delete()
        }
    }
    
    private fun callLlmWindows(prompt: String): String? {
        val tempDir = System.getProperty("java.io.tmpdir")
        val promptFile = File.createTempFile("prompt_", ".txt", File(tempDir))
        val script = File.createTempFile("call_llm_", ".bat", File(tempDir))
        
        try {
            promptFile.writeText(prompt)
            
            script.writeText("""
                @echo off
                cd /d "$tempDir"
                llm @"${promptFile.name}"
            """.trimIndent())
            
            val pb = ProcessBuilder()
            pb.command("cmd", "/c", script.absolutePath)
            pb.directory(File(tempDir))
            pb.redirectErrorStream(true)
            
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            
            if (exitCode == 0 && output.isNotBlank() && output.length > 50) {
                return output
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            promptFile.delete()
            script.delete()
        }
    }
    
    private fun callOllamaWindows(prompt: String): String? {
        val tempDir = System.getProperty("java.io.tmpdir")
        val promptFile = File.createTempFile("prompt_", ".txt", File(tempDir))
        val script = File.createTempFile("call_ollama_", ".bat", File(tempDir))
        
        try {
            promptFile.writeText(prompt)
            
            script.writeText("""
                @echo off
                cd /d "$tempDir"
                ollama run llama2 @"${promptFile.name}"
            """.trimIndent())
            
            val pb = ProcessBuilder()
            pb.command("cmd", "/c", script.absolutePath)
            pb.directory(File(tempDir))
            pb.redirectErrorStream(true)
            
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            
            if (exitCode == 0 && output.isNotBlank() && output.length > 50) {
                return output
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            promptFile.delete()
            script.delete()
        }
    }
    
    private fun callWindsurfWindows(prompt: String): String {
        val tempDir = System.getProperty("java.io.tmpdir")
        val promptFile = File.createTempFile("prompt_", ".txt", File(tempDir))
        val script = File.createTempFile("call_surf_", ".bat", File(tempDir))
        
        try {
            // Write prompt to temp file to avoid shell escaping issues
            promptFile.writeText(prompt)
            
            script.writeText("""
                @echo off
                cd /d "$tempDir"
                windsurf.cmd chat -m ask -- @"${promptFile.name}"
            """.trimIndent())
            
            val pb = ProcessBuilder()
            pb.command("cmd", "/c", script.absolutePath)
            pb.directory(File(tempDir))
            pb.redirectErrorStream(true)
            
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            
            if (exitCode != 0) {
                throw IllegalStateException("Windsurf call failed with exit code $exitCode: $output")
            }
            
            if (output.isBlank() || output.length < 50) {
                throw IllegalStateException("Windsurf returned empty or too short response")
            }
            
            return output
        } finally {
            promptFile.delete()
            script.delete()
        }
    }
    
    private fun callOpenCodeUnix(prompt: String): String {
        val home = System.getenv("HOME") ?: System.getProperty("user.home")
        val binPath = "$home/.npm-global/bin"
        val tempDir = "/tmp"
        
        val script = File.createTempFile("call_llm_", ".sh", File(tempDir))
        
        try {
            script.writeText("""
                #!/bin/bash
                cd "$tempDir"
                exec "$binPath/opencode" run --yes --model "$model" "$prompt"
            """.trimIndent())
            script.setExecutable(true)
            
            val pb = ProcessBuilder()
            pb.command(script.absolutePath)
            pb.directory(File(tempDir))
            pb.redirectErrorStream(true)
            
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            
            if (exitCode != 0) {
                throw IllegalStateException("LLM call failed with exit code $exitCode: $output")
            }
            
            if (output.isBlank() || output.length < 50) {
                throw IllegalStateException("LLM returned empty or too short response")
            }
            
            return output
        } finally {
            script.delete()
        }
    }
    
    private fun callMockMode(prompt: String): String {
        // Simulate some processing time
        Thread.sleep(1000)
        
        // Generate varied responses based on prompt content to simulate different AI outputs
        return when {
            prompt.contains("ITERATIVE") -> generateIterativeResponse()
            prompt.contains("authentication") -> generateAuthResponse()
            prompt.contains("validation") -> generateValidationResponse()
            prompt.contains("caching") -> generateCachingResponse()
            else -> generateBasicCrudResponse()
        }
    }
    
    private fun generateBasicCrudResponse(): String {
        return """
<file path="src/main/java/com/example/App.java">
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
</file>

<file path="src/main/java/com/example/Task.java">
package com.example;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String description;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Task() {}
    
    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
</file>

<file path="src/main/java/com/example/TaskRepository.java">
package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
}
</file>

<file path="src/main/java/com/example/TaskService.java">
package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
    
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }
    
    public Task createTask(Task task) {
        task.setUpdatedAt(java.time.LocalDateTime.now());
        return taskRepository.save(task);
    }
    
    public Task updateTask(Long id, Task taskDetails) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (optionalTask.isPresent()) {
            Task task = optionalTask.get();
            task.setTitle(taskDetails.getTitle());
            task.setDescription(taskDetails.getDescription());
            task.setCompleted(taskDetails.isCompleted());
            Task.setUpdatedAt(java.time.LocalDateTime.now());
            return taskRepository.save(task);
        }
        return null;
    }
    
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
</file>

<file path="src/main/java/com/example/TaskController.java">
package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Optional<Task> task = taskService.getTaskById(id);
        if (task.isPresent()) {
            return ResponseEntity.ok(task.get());
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return taskService.createTask(task);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task taskDetails) {
        Task updatedTask = taskService.updateTask(id, taskDetails);
        if (updatedTask != null) {
            return ResponseEntity.ok(updatedTask);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (taskService.deleteTask(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
</file>

<file path="src/main/resources/application.properties">
server.port=8080
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
</file>
        """.trim()
    }
    
    private fun generateIterativeResponse(): String {
        return """
<file path="src/main/java/com/example/TaskService.java">
package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
    
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }
    
    public Task createTask(Task task) {
        task.setUpdatedAt(java.time.LocalDateTime.now());
        return taskRepository.save(task);
    }
    
    public Task updateTask(Long id, Task taskDetails) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (optionalTask.isPresent()) {
            Task task = optionalTask.get();
            task.setTitle(taskDetails.getTitle());
            Task.setDescription(taskDetails.getDescription());
            Task.setCompleted(taskDetails.isCompleted());
            Task.setUpdatedAt(java.time.LocalDateTime.now());
            return taskRepository.save(task);
        }
        return null;
    }
    
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    // New method for authentication
    public List<Task> getTasksByUser(String username) {
        return taskRepository.findByUsername(username);
    }
    
    // Duplicate method pattern - similar to getAllTasks
    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }
    
    // Another duplicate pattern
    public List<Task> retrieveAllTasks() {
        return taskRepository.findAll();
    }
}
</file>

<file path="src/main/java/com/example/UserService.java">
package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public User createUser(User user) {
        user.setCreatedAt(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }
    
    public User updateUser(Long id, User userDetails) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            User.setUsername(userDetails.getUsername());
            User.setEmail(userDetails.getEmail());
            User.setPassword(userDetails.getPassword());
            return userRepository.save(user);
        }
        return null;
    }
    
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
</file>
        """.trim()
    }
    
    private fun generateAuthResponse(): String {
        return """
<file path="src/main/java/com/example/security/JwtTokenProvider.java">
package com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {
    
    @Value("\$\{app.jwtSecret}")
    private String jwtSecret;
    
    @Value("\$\{app.jwtExpirationInMs}")
    private int jwtExpirationInMs;
    
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
</file>

<file path="src/main/java/com/example/security/JwtAuthenticationFilter.java">
package com.example.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
</file>
        """.trim()
    }
    
    private fun generateValidationResponse(): String {
        return """
<file path="src/main/java/com/example/dto/TaskDto.java">
package com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TaskDto {
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private boolean completed;
    
    // Constructors
    public TaskDto() {}
    
    public TaskDto(String title, String description) {
        this.title = title;
        this.description = description;
        this.completed = false;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
</file>

<file path="src/main/java/com/example/validator/TaskValidator.java">
package com.example.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import com.example.dto.TaskDto;

@Component
public class TaskValidator implements Validator {
    
    @Override
    public boolean supports(Class<?> clazz) {
        return TaskDto.class.equals(clazz);
    }
    
    @Override
    public void validate(Object target, Errors errors) {
        TaskDto taskDto = (TaskDto) target;
        
        if (taskDto.getTitle() == null || taskDto.getTitle().trim().isEmpty()) {
            errors.rejectValue("title", "title.empty", "Title cannot be empty");
        }
        
        if (taskDto.getTitle() != null && taskDto.getTitle().length() < 3) {
            errors.rejectValue("title", "title.too.short", "Title must be at least 3 characters");
        }
        
        if (taskDto.getDescription() == null || taskDto.getDescription().trim().isEmpty()) {
            errors.rejectValue("description", "description.empty", "Description cannot be empty");
        }
    }
}
</file>
        """.trim()
    }
    
    private fun generateCachingResponse(): String {
        return """
<file path="src/main/java/com/example/cache/CacheService.java">
package com.example.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void put(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
    }
    
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }
}
</file>

<file path="src/main/java/com/example/service/CachedTaskService.java">
package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CachedTaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private CacheService cacheService;
    
    private static final String CACHE_PREFIX = "task:";
    
    public List<Task> getAllTasks() {
        String cacheKey = CACHE_PREFIX + "all";
        List<Task> cachedTasks = (List<Task>) cacheService.get(cacheKey);
        
        if (cachedTasks != null) {
            return cachedTasks;
        }
        
        List<Task> tasks = taskRepository.findAll();
        cacheService.put(cacheKey, tasks);
        return tasks;
    }
    
    public Optional<Task> getTaskById(Long id) {
        String cacheKey = CACHE_PREFIX + id;
        Task cachedTask = (Task) cacheService.get(cacheKey);
        
        if (cachedTask != null) {
            return Optional.of(cachedTask);
        }
        
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            cacheService.put(cacheKey, task.get());
        }
        return task;
    }
    
    public Task createTask(Task task) {
        Task createdTask = taskRepository.save(task);
        cacheService.delete(CACHE_PREFIX + "all");
        return createdTask;
    }
    
    public Task updateTask(Long id, Task taskDetails) {
        Optional<Task> optionalTask = TaskRepository.findById(id);
        if (optionalTask.isPresent()) {
            Task task = optionalTask.get();
            task.setTitle(taskDetails.getTitle());
            Task.setDescription(taskDetails.getDescription());
            Task.setCompleted(taskDetails.isCompleted());
            Task.setUpdatedAt(java.time.LocalDateTime.now());
            
            Task updatedTask = taskRepository.save(task);
            cacheService.delete(CACHE_PREFIX + id);
            cacheService.delete(CACHE_PREFIX + "all");
            return updatedTask;
        }
        return null;
    }
    
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            cacheService.delete(CACHE_PREFIX + id);
            cacheService.delete(CACHE_PREFIX + "all");
            return true;
        }
        return false;
    }
}
</file>
        """.trim()
    }
}
