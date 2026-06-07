# Coding Standards

> humanOS Native Android -- Kotlin and Compose Conventions
> Last updated: 2026-06-06

## Language

Kotlin is the only language for application code. No Java files in the project except auto-generated code.

## Style Guide

Follow the [Kotlin official coding conventions](https://kotlinlang.org/docs/coding-conventions.html) with the additions below.

## Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Composable functions | PascalCase | `TaskDetailScreen`, `CaptureCard` |
| Regular functions | camelCase | `fetchTasks`, `mapToEntity` |
| Variables and parameters | camelCase | `taskList`, `isLoading` |
| Constants (`const val`, top-level `val`) | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_SYNC_INTERVAL_MS` |
| Classes and interfaces | PascalCase | `TaskRepository`, `CaptureGateway` |
| Enum values | SCREAMING_SNAKE_CASE | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| Packages | lowercase, no underscores | `com.sinapsis.humanos.feature.capture` |
| Modules | kebab-case | `core-model`, `feature-capture`, `data-terrain` |
| Type parameters | Single uppercase letter or PascalCase | `T`, `InputType` |

## Package Structure per Module

Every module follows this internal structure:

```
feature-{name}/
  src/main/kotlin/com/sinapsis/humanos/feature/{name}/
    model/          -- UI models, sealed classes for UI state
    repository/     -- only in data modules; feature modules use domain interfaces
    ui/             -- Composables, ViewModels
      components/   -- reusable composables within the feature
    di/             -- Hilt module definitions
    navigation/     -- NavGraph contribution, route definitions
```

```
data-{name}/
  src/main/kotlin/com/sinapsis/humanos/data/{name}/
    model/          -- Room entities, DTOs
    repository/     -- Repository interfaces and implementations
    dao/            -- Room DAOs
    mapper/         -- Entity ↔ Domain model mappers
    di/             -- Hilt module definitions
```

```
core-{name}/
  src/main/kotlin/com/sinapsis/humanos/core/{name}/
    model/          -- domain models shared across modules
    di/             -- Hilt module definitions
    util/           -- utility classes (if any)
```

## Jetpack Compose Rules

### Stateless Composables

Composable functions are **stateless**. They receive state as parameters and emit events via lambdas. They never call `ViewModel` directly.

```kotlin
// CORRECT: stateless, receives state and callbacks
@Composable
fun TaskCard(
    task: TaskUiModel,
    onTaskClick: (String) -> Unit,
    onCompleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.clickable { onTaskClick(task.id) }) {
        // render task
    }
}

// WRONG: stateful, calls ViewModel directly
@Composable
fun TaskCard(viewModel: TaskViewModel) {
    val task by viewModel.task.collectAsState()
    Card(modifier = Modifier.clickable { viewModel.onTaskClick() }) {
        // ...
    }
}
```

### State Hoisting

State lives in the ViewModel. Screen-level composables connect ViewModel state to stateless children.

```kotlin
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    TaskListContent(
        tasks = uiState.tasks,
        isLoading = uiState.isLoading,
        onTaskClick = viewModel::onTaskClick,
        onRefresh = viewModel::refresh
    )
}

@Composable
fun TaskListContent(
    tasks: List<TaskUiModel>,
    isLoading: Boolean,
    onTaskClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pure UI, no ViewModel reference
}
```

### Modifier Parameter

Every composable that emits UI accepts a `modifier: Modifier = Modifier` as its **last** parameter (or first if there are many parameters and the composable is a layout).

### Preview

Every screen-level composable has at least one `@Preview` function.

```kotlin
@Preview(showBackground = true)
@Composable
fun TaskListContentPreview() {
    HumanosTheme {
        TaskListContent(
            tasks = previewTasks,
            isLoading = false,
            onTaskClick = {},
            onRefresh = {}
        )
    }
}
```

## ViewModel Conventions

- One `ViewModel` per screen.
- Expose UI state as `StateFlow<UiState>` (not `LiveData`).
- Use `sealed interface` for UI state.
- Side effects via `SharedFlow<UiEvent>` (one-time events like navigation, snackbar).

```kotlin
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskListUiState>(TaskListUiState.Loading)
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskListEvent>()
    val events: SharedFlow<TaskListEvent> = _events.asSharedFlow()

    init {
        loadTasks()
    }

    fun onTaskClick(taskId: String) {
        viewModelScope.launch {
            _events.emit(TaskListEvent.NavigateToDetail(taskId))
        }
    }
}

sealed interface TaskListUiState {
    data object Loading : TaskListUiState
    data class Success(val tasks: List<TaskUiModel>) : TaskListUiState
    data class Error(val message: String) : TaskListUiState
}

sealed interface TaskListEvent {
    data class NavigateToDetail(val taskId: String) : TaskListEvent
}
```

## Dependency Injection

- Hilt for all DI. No manual service locators.
- `@Module` + `@InstallIn(SingletonComponent::class)` for app-scoped singletons.
- `@Module` + `@InstallIn(ViewModelComponent::class)` for ViewModel-scoped.
- Prefer constructor injection. Use `@Inject` on the constructor.
- Interfaces bound with `@Binds` (not `@Provides` for simple interface-to-implementation).

## Comments

- **No comments unless WHY is non-obvious.** Code should be self-documenting.
- Never comment WHAT the code does. If the code needs a WHAT comment, rename things instead.
- Use KDoc (`/** */`) for public API surfaces (interfaces, public functions in core modules).
- TODO format: `// TODO(TASK-XXX): description` -- every TODO must reference a task.
- FIXME format: `// FIXME(TASK-XXX): description` -- same rule.
- No commented-out code. Delete it; git remembers.

## Error Handling

- Use `Result<T>` or sealed classes for expected errors. Not exceptions.
- Exceptions only for truly exceptional cases (programmer errors, infrastructure failures).
- Never catch `Exception` or `Throwable` broadly. Catch specific types.
- Coroutine `CoroutineExceptionHandler` at the ViewModel scope level for unhandled errors.

## Coroutines

- `viewModelScope` for ViewModel coroutines.
- `Dispatchers.IO` for disk/network operations (injected, not hardcoded).
- `Dispatchers.Default` for CPU-intensive work.
- Never use `GlobalScope`.
- Prefer `Flow` over `suspend` for streams of data. Use `suspend` for one-shot operations.

## Enforcement

| Tool | Purpose | CI Gate |
|---|---|---|
| **ktlint** | Code formatting (Kotlin official style) | Fail on violations |
| **detekt** | Static analysis (complexity, code smells) | Fail on new violations |
| **Android Lint** | Android-specific checks | Fail on error-level |
| **Compose Lint** | Compose-specific checks (stability, recomposition) | Fail on error-level |

### ktlint Configuration

Standard Kotlin style guide rules. No custom rules. Applied via `org.jlleitschuh.gradle.ktlint` plugin.

### detekt Configuration

Default rule set with these overrides:
- `complexity.LongMethod` threshold: 30 lines (default 60)
- `complexity.LongParameterList` threshold: 8 (default 6, because Compose functions have many params)
- `complexity.ComplexCondition` threshold: 4
- `naming.FunctionNaming` excludes: `*Test.kt` (test functions use backtick names)

## References

- ADR-0002: Module architecture
- [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
