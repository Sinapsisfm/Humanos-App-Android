# Testing Strategy

> humanOS Native Android -- Test Pyramid and Tooling
> Last updated: 2026-06-06

## Test Pyramid

```
         /  UI Tests  \          ← Few, slow, high confidence
        / Integration   \
       /   Tests          \
      /                    \
     /    Unit Tests         \   ← Many, fast, focused
    /________________________\
```

## Tooling

| Layer | Framework | Purpose |
|---|---|---|
| Unit tests | **JUnit 5** | Test framework (assertions, lifecycle, parameterized) |
| Mocking | **MockK** | Kotlin-first mocking (coroutines, extension functions) |
| Flow testing | **Turbine** | Test `Flow` emissions with structured assertions |
| Coroutine testing | **kotlinx-coroutines-test** | `TestDispatcher`, `runTest`, `advanceUntilIdle` |
| Instrumented tests | **AndroidX Test** | Android framework dependencies on device/emulator |
| DI in tests | **Hilt Testing** | `@HiltAndroidTest`, `@UninstallModules`, `@BindValue` |
| UI tests | **Compose Testing** | `composeTestRule`, semantic assertions, actions |
| Room tests | **Room Testing** | In-memory database for DAO tests |

## Test Types

### Unit Tests (`test/`)

Fast, isolated, run on JVM. No Android framework dependencies.

**What to unit test:**
- Repository implementations (with fake data sources)
- Use cases / domain logic
- Mappers (entity <-> domain model)
- ViewModel state transitions
- Utility functions
- Validators and formatters

**Pattern:**

```kotlin
class TaskRepositoryImplTest {

    private val fakeTaskDao = FakeTaskDao()
    private val fakeRemoteDataSource = FakeTaskRemoteDataSource()
    private val repository = TaskRepositoryImpl(fakeTaskDao, fakeRemoteDataSource)

    @Test
    fun `getTasks returns mapped domain models from local source`() = runTest {
        // Given
        fakeTaskDao.insertAll(listOf(taskEntity1, taskEntity2))

        // When
        val result = repository.getTasks().first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("Task 1")
    }
}
```

### ViewModel Tests

Test state transitions using Turbine for Flow assertions.

```kotlin
class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeGetTasksUseCase = FakeGetTasksUseCase()
    private lateinit var viewModel: TaskListViewModel

    @Test
    fun `initial state is Loading then transitions to Success`() = runTest {
        // Given
        fakeGetTasksUseCase.setResult(listOf(task1, task2))
        viewModel = TaskListViewModel(fakeGetTasksUseCase)

        // Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(TaskListUiState.Loading::class.java)
            val success = awaitItem() as TaskListUiState.Success
            assertThat(success.tasks).hasSize(2)
        }
    }
}
```

### Instrumented Tests (`androidTest/`)

Run on device or emulator. Required when Android framework is needed.

**What to instrumented test:**
- Room DAO operations (in-memory database)
- DataStore read/write
- WorkManager workers (with `TestWorkerBuilder`)
- Navigation graph correctness
- Integration between real Hilt components

**Room DAO test pattern:**

```kotlin
@HiltAndroidTest
class TaskDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var database: HumanosDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HumanosDatabase::class.java
        ).build()
        taskDao = database.taskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTask() = runTest {
        taskDao.insert(testTaskEntity)
        val result = taskDao.getById(testTaskEntity.id).first()
        assertThat(result?.title).isEqualTo(testTaskEntity.title)
    }
}
```

### UI Tests (Compose Testing)

Test composable behavior using semantic tree assertions.

```kotlin
class TaskListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTaskListWhenLoaded() {
        composeTestRule.setContent {
            HumanosTheme {
                TaskListContent(
                    tasks = listOf(
                        TaskUiModel(id = "1", title = "Review chart", status = "Pending"),
                        TaskUiModel(id = "2", title = "Submit report", status = "Done")
                    ),
                    isLoading = false,
                    onTaskClick = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Review chart").assertIsDisplayed()
        composeTestRule.onNodeWithText("Submit report").assertIsDisplayed()
    }

    @Test
    fun showsLoadingIndicatorWhenLoading() {
        composeTestRule.setContent {
            HumanosTheme {
                TaskListContent(
                    tasks = emptyList(),
                    isLoading = true,
                    onTaskClick = {},
                    onRefresh = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }
}
```

## Fakes vs Mocks

**Prefer fakes from `testing-common` over mocks.**

| Approach | When to Use |
|---|---|
| **Fake** (manual test double) | Default choice. Implement the interface with in-memory behavior. Reusable across tests. |
| **Mock** (MockK) | When the interface has many methods and you only care about one. When verifying interaction (was method called?). |

### testing-common Module

The `testing-common` module provides shared test infrastructure:

```
testing-common/
  src/main/kotlin/.../testing/
    fakes/
      FakeTaskRepository.kt
      FakeTaskDao.kt
      FakeCaptureRepository.kt
      FakeSyncManager.kt
      FakeHealthConnectGateway.kt
    rules/
      MainDispatcherRule.kt       -- replaces Main dispatcher with TestDispatcher
    fixtures/
      TaskFixtures.kt             -- factory functions for test data
      CaptureFixtures.kt
    assertions/
      FlowAssertions.kt           -- custom Turbine helpers
```

### Fake Example

```kotlin
class FakeTaskRepository : TaskRepository {
    
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var shouldFail = false

    fun setTasks(taskList: List<Task>) {
        tasks.value = taskList
    }

    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    override fun getTasks(): Flow<List<Task>> {
        if (shouldFail) throw IOException("Fake network error")
        return tasks
    }

    override suspend fun getTaskById(id: String): Task? {
        return tasks.value.find { it.id == id }
    }
}
```

## Coverage Targets

| Module Category | Target | Rationale |
|---|---|---|
| `core-*` modules | **80%** | Shared foundation; bugs here cascade everywhere |
| `data-*` modules | **80%** | Data integrity is critical |
| `domain-*` modules | **80%** | Business logic must be correct |
| `feature-*` modules | **60%** | UI code is harder to unit test; covered by UI tests |
| `integration-*` modules | **60%** | Heavy SDK dependencies; mock boundaries tested |
| `app` module | **40%** | Mostly wiring; covered by instrumented tests |

Coverage measured by **JaCoCo** and reported in CI. Coverage is a guideline, not a gate -- meaningful tests matter more than percentage.

## CI Integration

### PR Workflow

```
On every PR to develop:
  1. ktlint check
  2. detekt check
  3. Android Lint
  4. testDebugUnitTest (all unit tests)
  5. JaCoCo coverage report
  6. Build debug APK (verify compilation)
```

### Nightly Workflow

```
Nightly on develop:
  1. All unit tests
  2. Instrumented tests on emulator (API 34)
  3. Full JaCoCo coverage report
  4. Dependency vulnerability scan
```

### Test Tags

```kotlin
@Tag("slow")           // Tests that take > 5 seconds
@Tag("requiresDevice") // Must run on physical device (sensors, Health Connect)
@Tag("flaky")          // Known flaky, excluded from CI gate but tracked
```

## Test Naming

Use backtick-quoted descriptive names:

```kotlin
@Test
fun `repository returns empty list when no tasks in database`() { }

@Test
fun `sync worker retries on network failure`() { }

@Test
fun `capture upload skips already-uploaded items`() { }
```

## What NOT to Test

- Private functions (test via public API)
- Hilt module bindings (covered by compilation + integration tests)
- Generated code (Room DAOs SQL, Hilt components)
- Third-party library internals
- Simple data classes with no logic

## References

- CODING_STANDARDS.md: Naming conventions for test files
- COMMIT_GUIDELINES.md: `test` commit type
- ADR-0002: Module architecture (testing-common module)
- [MockK documentation](https://mockk.io/)
- [Turbine documentation](https://github.com/cashapp/turbine)
- [Compose testing](https://developer.android.com/develop/ui/compose/testing)
