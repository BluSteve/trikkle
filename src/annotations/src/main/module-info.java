module annotations {
	requires java.compiler; // Required for annotation processing

	exports org.trikkle; // Export the package containing your annotation
	provides javax.annotation.processing.Processor with org.trikkle.Processor;
}
