ShowcaseView
---

The ShowcaseView (SCV) library is designed to highlight and showcase specific parts of apps to the user with a distinctive and attractive overlay. This library is great for pointing out points of interest for users, gestures, or obscure but useful items.

[Click to the original ShowcaseView](https://github.com/amlcurran/ShowcaseView)
---

**My modification:**
- Multiple target(or multiple showcases)
~~~
sv = new ShowcaseView.Builder(this)
        .setTarget(ViewTarget1, ViewTarget2, ViewTarget3...)
        ...
        .build();
~~~
- Set the text's position more precisely
~~~
int moreLeft = -30;
int moreDown = 100;
sv = new ShowcaseView.Builder(this)
        .setContentText("My text example.")
        .setCompensationTextPosition(moreLeft, moreDown)
        ...
        .build();
~~~
- Use any view in action for close not only button
~~~
sv = new ShowcaseView.Builder(this)
        .replaceEndButton(MyCustomView);
        ...
        .build();
~~~
- Compatible for min sdk 11+ to 10+

**CAUTION:**
 my modifications may have caused malfunction of some functions that were not my focus.


Copyright and Licensing
----
This library is distributed under an Apache 2.0 License.
