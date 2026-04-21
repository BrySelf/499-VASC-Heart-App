import numpy as np

# CLASS TAKEN FROM SAMUEL PRÖLL

class LiveFilter:
    def process(self, x):
        """Process new value and return filtered value."""
        if np.isnan(x):
            return x
        
        return self._process(x)
    
    def __call__(self, x):
        return self.process(x)
    
    def _process(self, x):
        """Implement in subclass to process new value."""
        raise NotImplementedError("Derived classes must implement _process method")