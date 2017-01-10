package trikita.anvil;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyInt;

@TargetApi(Build.VERSION_CODES.KITKAT)
@RunWith(PowerMockRunner.class)
@PrepareForTest({View.class, ViewGroup.class, FrameLayout.class, Utils.MockLayout.class, Utils.MockView.class})
abstract public class Utils implements Anvil.ViewFactory {

    Map<Class, Integer> createdViews = new HashMap<>();
    Map<String, Integer> changedAttrs = new HashMap<>();

    Utils() {
        mockViewFactory(this);
    }

    protected void mockViewFactory(Anvil.ViewFactory viewFactory) {
        // Mock default view factory
        try {
            Field factory = Anvil.class.getDeclaredField("viewFactory");
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(factory, factory.getModifiers() & ~Modifier.FINAL);
            factory.setAccessible(true);
            factory.set(null, viewFactory);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public View fromClass(Context c, Class<? extends View> v) {
        try {
            createdViews.put(v, !createdViews.containsKey(v) ? 1 : (createdViews.get(v) + 1));
            View mocked = Mockito.spy(v.getConstructor(Context.class).newInstance(getContext()));
            if (mocked instanceof MockLayout) {
                mockFindViewWithTags((MockLayout) mocked);
            }
            return mocked;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public View fromXml(Context c, int xmlId) {
        return null;
    }

    public View fromId(View v, int viewId) {
        if (v instanceof MockLayout) {
            for (View child : ((MockLayout) v).children) {
                if (child.getId() == viewId) {
                    return child;
                }
            }
        }
        return null;
    }

    Anvil.Renderable empty = new Anvil.Renderable() {
        public void view() {
        }
    };

    MockLayout container;

    @Before
    public void setUp() {
        changedAttrs.clear();
        createdViews.clear();
        container = Mockito.spy(new MockLayout(getContext()));
        mockFindViewWithTags(container);
    }

    @After
    public void tearDown() {
        Anvil.unmount(container);
    }

    public void mockFindViewWithTags(MockLayout mockedView) {
        PowerMockito
                .doAnswer(new Answer<Object>() {
                    @Override
                    public View answer(InvocationOnMock invocation) throws Throwable {
                        Object tag = invocation.getArgumentAt(0, Object.class);
                        MockLayout mockLayout = (MockLayout) invocation.getMock();
                        for (View child : mockLayout.children) {
                            if (child.getTag().equals(tag)) {
                                return child;
                            }
                        }
                        return null;
                    }
                })
                .when(mockedView)
                .findViewWithTag(anyInt());
    }

    public Context getContext() {
        return null;
    }

    public static class MockView extends View {
        public final Map<String, Object> props = new HashMap<>();

        public MockView(Context c) {
            super(c);
        }

        public void setId(int id) {
            this.props.put("id", id);
        }

        public int getId() {
            return (Integer) this.props.get("id");
        }

        public void setTag(Object tag) {
            this.props.put("tag", tag);
        }

        public Object getTag() {
            return this.props.get("tag");
        }
    }

    public static class MockLayout extends FrameLayout {
        public final Map<String, Object> props = new HashMap<>();
        private List<View> children = new ArrayList<>();

        public MockLayout(Context c) {
            super(c);
        }

        public int getChildCount() {
            return children.size();
        }

        public View getChildAt(int index) {
            return children.get(index);
        }

        public int indexOfChild(View v) {
            return children.indexOf(v);
        }

        public void addView(View v, int index) {
            this.children.add(index, v);
            super.addView(v, index);
        }

        public void removeView(View v) {
            this.children.remove(v);
            super.removeView(v);
        }

        public void removeViews(int start, int count) {
            if (count > 0) {
                this.children.subList(start, start + count).clear();
            }
            super.removeViews(start, count);
        }

        public void setId(int id) {
            this.props.put("id", id);
        }

        public int getId() {
            return (Integer) this.props.get("id");
        }

        public void setTag(Object tag) {
            this.props.put("tag", tag);
        }

        public Object getTag() {
            return this.props.get("tag");
        }
    }

    PropFunc propFuncInstance = new PropFunc();

    public Void prop(String name, Object value) {
        return BaseDSL.attr(propFuncInstance, new AbstractMap.SimpleImmutableEntry<>(name, value));
    }

    // AttrFunc implementation for props<k,v>
    public class PropFunc implements Anvil.AttrFunc<Map.Entry<String, Object>> {
        public void apply(View v, Map.Entry<String, Object> x, Map.Entry<String, Object> old) {
            String key = x.getKey();
            changedAttrs.put(key, !changedAttrs.containsKey(key) ? 1 : (changedAttrs.get(key) + 1));
            if (v instanceof MockView) {
                ((MockView) v).props.put(x.getKey(), x.getValue());
            } else if (v instanceof MockLayout) {
                ((MockLayout) v).props.put(x.getKey(), x.getValue());
            }
        }
    }
}
