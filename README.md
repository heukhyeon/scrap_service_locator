# ScrapServiceLocator

_A small, asynchronous IoC library for Android that might be half compile-time safe (?)_

Before I start, I'm very poor at English, so please forgive the parts of the README that may not be properly understood.

Although strongly influenced by [Koin](https://github.com/InsertKoinIO/koin),

I made it because it was too cumbersome to manually change module dependency definitions one by one in code that changes hundreds of lines a day.

[Hilt](https://dagger.dev/hilt/) also exists as an option, but

1. I hate lateinit,
2. It is cumbersome to annotate each component that needs to be injected with @Inject.
3. It was unbearable for all component classes to be initialized on the main thread.
4. It was a waste of time to touch the bytecode for an apk over 100MB at compile time.

So, as a simple trick using kapt , I wanted to make the dependency injection structure as simple as possible.


## How to use ( Gradle Settings )


1. Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

2. Add Depdendencies in Your application level `build.gradle`

[![Release](https://jitpack.io/v/heukhyeon/scrap_service_locator.svg)](https://jitpack.io/#heukhyeon/scrap_service_locator)

```
plugins {
...
    id 'kotlin-kapt'
}

android {

...
// If you are using `serviceLocator-Android` you need to enable ViewBinding.
    buildFeatures {
        viewBinding = true
    }
}

// The corresponding kapt option is required by all modules. 
// If you are too lazy to write this down, please refer to `kapt_sample.gradle` in the repository. It might be helpful.
kapt {
    arguments {
        arg("moduleName", 'Your module-specific class name')
    }
}

dependencies {
    def latest_version = '0.0.4-alpha-fix'
...
    implementation "com.github.heukhyeon:scrap_service_locator:$latest_version"
    implementation "com.github.heukhyeon:scrap_service_locator_android:$latest_version"
    kapt "com.github.heukhyeon:scrap_service_locator:$latest_version"
    
    // The dependencies below are optional. Please refer to the wiki for what each role does.
    implementation "com.github.heukhyeon:scrap_service_locator_android_viewbinding:$latest_version"
    implementation "com.github.heukhyeon:scrap_service_locator_android_fragment:$latest_version"
    implementation "com.github.heukhyeon:scrap_service_locator_android_recyclerview:$latest_version"
}
```


## How to Use (in Kotlin)

I think it would be good if you take a look around the [sample project](https://github.com/heukhyeon/ScrapServiceLocator/tree/main/sample).


The basic dependency injection method is the same as that of Koin.

For classes that cannot control the constructor (activities, fragments, etc.), use the `inject()` delegate function,
```kotlin
// if Activity, implements ActivityInitializer, if Fragment, implements FragmentInitializer
class SampleActivity : AppCompatActivity(), ActivityInitializer {

   ...
    private val presenter by inject(SamplePresenter::class)
}
```

Classes that can control constructors (most classes) use `constructor` injection.

```kotlin
class SamplePresenter(
    private val sampleRepository: SampleRepository // Interface.
) {
  ...
}
```

Okay, will you look pretty? What more should we do here?

Let me explain it step by step.

### 1. All classes that need to be injected must be annotated with @Component.

In the example above, `SamplePresenter` should be injected into `SampleActivity`, so it should be annotated with @Component.

```kotlin
import kr.heukhyeon.service_locator.Component

@Component
class SamplePresenter(
    private val sampleRepository: SampleRepository
) {
  ...
}
```

With some exceptions, all classes included in the constructor of a class annotated with @Component must be annotated with @Component.

Taking the above example again, would `SampleRepository` also need the @Component annotation?


### 2. Instead of adding @Component annotation to the interface, add @Component annotation to the class to be injected.
```kotlin
// Types referenced in the presentation layer
interface SampleRepository {
    fun getTestText(): String
    suspend fun putLatestClickedTime() : String
}

// A data layer class that is actually injected, but does not exist in the Presentation Layer
class SampleRepositoryImpl : SampleRepository {

    private val time = System.currentTimeMillis()
    private var latestClickedTime : Long? = null

    override fun getTestText(): String {
        // BLABLA...
    }

    override suspend fun putLatestClickedTime(): String {
        // BLABLA...
    }
}
```

The implementation of SampleRepository must exist at runtime, but on the contrary, its higher layer (such as Presenter or Activity) must not know about its existence.

So while annotating @Component, we add a few extras.

```kotlin
@Component(scope = ComponentScope.IS_SINGLETON, bind = SampleRepository::class)
class SampleRepositoryImpl : SampleRepository {
}
```

- scope : Set the caching policy for requests for the same type of component.
  - `SHARED_IF_EQUAL_OWNER` :  This is the default. Components with the same ComponentOwner share components.
  - `IS_SINGLETON` : Once created, an object is returned for every request during the application cycle.
  - `NOT_CACHED` : Whenever a request comes in, a new object is created. **Don't use it except in cases like RecyclerView.ViewHolder.**

- bind : Specifies what type of class this class will be returned for dependency requests.

The default value is returned only when requesting a dependency on itself (SampleRepositoryImpl).


### 3. Add `@ApplicationEntryPoint` annotation to your Application class and call `RootInjector.initialize(this)` at onCreate time.

<pre>
// You don't necessarily have to implement AndroidInitializer in your Application.
// This is explained in wiki
@ApplicationEntryPoint
class SampleApp : Application(), AndroidInitializer {

...
    override fun onCreate() {
        super.onCreate()
        // IMPORTANT
        <b>RootInjector.initialize(this)</b>
	// If you are using the `service_locator_android` library, you must call this.
	<b>InjectLifecycleManager.initialize(this)</b>
        startInitialize()
    }
}
</pre>

### 4. Move your initialization logic after overriding the onInitialize function.

```kotlin
class SampleActivity : AppCompatActivity(), ActivityInitializer {

    private val presenter by inject(SamplePresenter::class)
    private val binding by inject(ActivitySampleBinding::class)

...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_loading)

        // AS-IS, BAD !!!!
        onInit()
    }

    override suspend fun onInitialize() {
        super.onInitialize()
        withContext(Dispatchers.Main) {

            // TO-BE, GOOD!
            onInit()
        }
    }

    // My Initialize Function
    private fun onInit() {
        binding.textView.text = presenter.getTestText()
        binding.updateButtonView.setOnClickListener {
            binding.updateButtonView.isEnabled = false
            binding.loadingView.visibility = View.VISIBLE
            getCoroutineScope().launch {
                updateTime()
            }
        }
    }

...

```

### 5. If you are using proguard, add the following statement to your proguard rules.
```
-keepclassmembers class * extends kr.heukhyeon.service_locator.RootInjector {
    public <init>(android.content.Context);
}
```



More details that are not explained in the above and sample projects are described in the [Wiki](https://github.com/heukhyeon/ScrapServiceLocator/wiki).
