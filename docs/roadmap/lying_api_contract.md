Yes—API contracts are one of the **cleanest, highest-signal “intent vs reality” checks you can add**, and they work *early*, even before tests or PRs exist.

The key is to treat the endpoint itself as a **self-declared contract** and verify whether the implementation actually honors it.

---

# 🧠 Core Idea: “Endpoint Truthfulness”

Every REST endpoint declares intent through:

* Route (`/users`, `/users/{id}`)
* HTTP method (`GET`, `POST`, etc.)
* Parameters (`page`, `size`, `filter`)
* Annotations (`@Transactional`, `@Secured`, etc.)
* Naming (`getUsers`, `createUser`)
* Documentation (`@Operation`, comments)

👉 That’s already a **mini spec embedded in code**

Your job is:

> **Detect when implementation violates its own declared contract**

---

# 🔴 1. Parameter–Behavior Mismatch (Highest ROI)

## Example:

```java
@GetMapping("/users")
public List<User> getUsers(
    @RequestParam int page,
    @RequestParam int size
) {
    return userRepository.findAll(); // ignores pagination
}
```

## Detection:

* Parameters exist
* Not referenced in method body

👉 **Strong drift signal**

---

## General Rule:

> “Declared inputs must influence execution”

Applies to:

* pagination
* filters
* sorting
* feature flags

---

# 🔴 2. HTTP Method Semantics Violations

REST has strong expectations:

| Method | Expected Behavior |
| ------ | ----------------- |
| GET    | read-only         |
| POST   | create            |
| PUT    | full update       |
| PATCH  | partial update    |
| DELETE | delete            |

---

## Example:

```java
@GetMapping("/users/delete/{id}")
public void deleteUser(...) { ... }
```

👉 GET performing mutation

---

## Or:

```java
@PostMapping("/users")
public List<User> listUsers() { ... }
```

👉 POST used for retrieval

---

## Detection:

* Method type vs detected side effects

  * DB writes (`save`, `delete`)
  * state mutation

👉 This is **high-confidence semantic drift**

---

# 🔴 3. Route Naming vs Implementation

## Example:

```java
@GetMapping("/users/{id}")
public User getUser(Long id) {
    return userRepository.findAll().get(0); // ignores id
}
```

## Detection:

* Path variables not used
* Route implies specificity (`{id}`) but implementation ignores it

---

## Another:

```java
@PostMapping("/users/bulk")
public void bulkCreate(...) {
    createSingleUser(); // not bulk
}
```

---

# 🟠 4. Annotation vs Behavior Drift

Annotations encode intent.

---

## Example: Security

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(...) { ... }
```

But:

* No auth context used
* Or method bypassed internally

---

## Example: Transactions

```java
@Transactional
public void updateUser(...) {
    // no DB interaction
}
```

---

## Or missing where expected:

* write operations without `@Transactional`

---

👉 These mismatches are subtle but high-value

---

# 🟠 5. API Documentation vs Code

If using:

* Swagger / OpenAPI annotations
* Javadoc

---

## Example:

```java
@Operation(summary = "Returns paginated users")
```

But:

```java
return findAll(); // no pagination
```

---

## Detection:

* Extract keywords from docs
* Compare to implementation signals

---

👉 This is **spec drift inside the code itself**

---

# 🟡 6. Response Shape vs Contract

## Example:

```java
@GetMapping("/users")
public User getUsers() { ... } // singular return
```

But route implies list

---

Or:

* returns raw entity instead of DTO
* missing fields promised in docs

---

👉 This becomes very strong if combined with OpenAPI/TypeSpec

---

# 🟠 7. Error Handling Contract Violations

## Example:

```java
@GetMapping("/users/{id}")
public User getUser(...) {
    return repo.findById(id).orElse(null);
}
```

But expected:

* 404 if not found

---

## Detection:

* Null return instead of proper HTTP response
* Missing exception handling

---

👉 This ties directly into your earlier **null propagation detection**

---

# 🧪 8. Test ↔ API Contract Alignment (Power Combo)

Now combine with tests:

---

## Example:

Test:

```java
shouldReturn404WhenUserNotFound()
```

API:

* returns null instead

---

👉 Now you have:

* contract mismatch
* test mismatch

**Double confirmation = very high confidence**

---

# 🧩 9. Building an “API Integrity Score”

You can aggregate signals like:

| Check                   | Weight |
| ----------------------- | ------ |
| Unused parameters       | +25    |
| HTTP semantic violation | +25    |
| Route-variable unused   | +20    |
| Doc vs code mismatch    | +15    |
| Annotation mismatch     | +10    |
| Response inconsistency  | +10    |

---

## Output:

> **API Contract Integrity: 62%**
>
> Issues:
>
> * `/users` declares pagination but does not implement it
> * `/users/{id}` ignores path variable
> * GET endpoint performs mutation
> * 2 endpoints return null instead of proper HTTP responses

---

# 🚀 Why This Layer Is So Powerful

This is where your tool starts to feel *intelligent*, not just analytical.

Because you’re detecting:

> **“The API is lying about what it does”**

That’s:

* easy for devs to understand
* hard for existing tools to catch well
* extremely common in AI-generated code

---

# ⚖️ How This Fits Into Your Bigger System

This becomes another **intent signal layer**:

| Layer                 | Role              |
| --------------------- | ----------------- |
| Spec (.md / TypeSpec) | external intent   |
| Tests                 | executable intent |
| API contract          | embedded intent   |
| Code                  | actual behavior   |

---

Drift = **misalignment across layers**

---

# 💡 Advanced Direction (If You Want to Go Further)

You could eventually build:

### “Endpoint Truth Graph”

For each endpoint:

* Declared:

  * route
  * params
  * docs
* Observed:

  * DB calls
  * logic patterns
  * side effects

Then compute:

> **Truthfulness score per endpoint**

---

# 🔥 Bottom Line

This idea is absolutely worth pursuing because:

* ✅ Works early (no PR needed)
* ✅ Doesn’t require external data
* ✅ Highly explainable
* ✅ Strong signal for AI-generated issues

---

If you combine:

* API contract checks
* test alignment
* spec coverage

You end up with something very few tools can do:

> **“Detect when software *claims* to do something it doesn’t.”**

That’s a genuinely differentiated capability.
