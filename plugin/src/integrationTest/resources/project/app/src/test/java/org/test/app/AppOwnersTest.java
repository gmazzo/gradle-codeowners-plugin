package org.test.app;

import org.junit.Test;
import org.test.lib.LibClass;
import org.test.utils.AppUtils;
import org.test.utils.LibUtils;

import static io.github.gmazzo.codeowners.CodeOwners.getCodeOwners;
import static org.junit.Assert.assertEquals;
import static kotlin.collections.SetsKt.setOf;

/**
 * On <code>debug</code> both <code>app</code> and <code>lib</code>s have owners computed
 * On <code>release</code>, the build script on <code>app</code> is set up to not to compute them,
 * so we only have the ones from <code>lib</code>s
 */
public class AppOwnersTest {

    @Test
    public void ownerOfApp() {
        assertEquals(BuildConfig.DEBUG ? setOf("android-devs") : null, getCodeOwners(AppClass.class));
    }

    @Test
    public void ownerOfAppUtils() {
        assertEquals(BuildConfig.DEBUG ? setOf("android-devs") : setOf("kotlin-devs"), getCodeOwners(AppUtils.class));
    }

    /**
     * Known limitation test: owners if same package but different sources gets merged
     */
    @Test
    public void ownerOfAppUtilsPackage() {
        assertEquals(
                BuildConfig.DEBUG ? setOf("android-devs", "kotlin-devs") : setOf("kotlin-devs"),
                getCodeOwners(AppUtils.class.getClassLoader(), AppUtils.class.getPackage().getName()));
    }

    @Test
    public void ownerOfLib() {
        assertEquals(setOf("kotlin-devs"), getCodeOwners(LibClass.class));
    }

    @Test
    public void ownerOfLibUtils() {
        assertEquals(BuildConfig.DEBUG ? setOf("kotlin-devs", "android-devs") : setOf("kotlin-devs"), getCodeOwners(LibUtils.class));
    }

}
