#Ferro

Simple and powerful MVP library for Android 

Ferro elegantly solves two age-old problems of Android Framework related to configuration changes:
* restore screen's data
* managing background tasks

First problem solved using permanent presenter, second - using freezing rx events (Observable doesn't unsubscribe).
Also new feature added - freeze rx event when screen becomes invisible, and defreeze it when screen goes to foreground.

The schematic work of ferro:

![SchematicImage](ferro.gif)

Ferro is divided into 3 layers, each of which adds behavior to the previous. So you can use only part of ferro.

The first layer:
##ferro-core
This library contains base classes for Activity and Fragment (`PSSActivity, PSSFragmentV4`, PSS - persistent screen scope). For each activity and fragment, based on this classes, will be created `PersistentScreenScope`.  You can get it by calling the method `PSSActivity#getPersistentScreenScope` or `PSSFragmentV4#getPersistentScreenScope`. This object isn't destroyed, when configuration changed, and destroyed when screen finally destroyed(e.g. after call `Activity#finish()`). You can add listener, which called when `PersistentScreenScope` destroyed. It has methods for storing and getting objects.
In reality, `PersistentScreenScope` is retained fragment without view.

This mechanism is ideal for storing presenter, that is done in the next extention:
##ferro-mvp
This library contains base classes for view, presenter and screen component. For each screen you need extend `ScreenComponent`, `MvpPresenter` and `MvpActivityView` or `MvpFragmentV4View`. The `ScreenComponent` will be saved in `PersistentScreenScope` and reused when view recreated.
In method `ScreenComponent#inject(view)` you need insert presenter to the view. For this purpose the easiest way to use dagger component as `ScreenComponent`. 

Method `MvpPresenter#onLoad(viewRecreated)` will be called, when view is ready, flag `viewRecreated` means, that view is recreated after configuration change. In this method you need show on view previously loaded data if it exist.

If you use dagger, this library contains two scope annotations `@PerApplication` and `@PerScreen`. It also contains `ActivityProvider` and `FragmentProvider`, which can be used for getting access to Activity or Fragment inside objects, provided by dagger screen component (e.g. inside Navigator class).



