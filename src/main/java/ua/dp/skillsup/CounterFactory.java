package ua.dp.skillsup;

/**
 * @author Andrey Lomakin <a href="mailto:lomakin.andrey@gmail.com">Andrey Lomakin</a>
 * @since 03/11/14
 */
public class CounterFactory {
	public enum CounterType {
		ATOMIC, FAST, MY
	}

	public static Counter build(CounterType type) {
		switch (type) {
			case ATOMIC:
				return new AtomicCounter();
            case MY:
                return new MyCounter(4);
			/* case MY_LAZY:
                return new MyLazyCounter(4);*/
			case FAST:
				return new FastCounter(4);
		}

		throw new IllegalArgumentException();
	}
}