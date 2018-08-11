#Create data


#set.seed(112)
#data=matrix(sample(1:30,15) , nrow=3)

data = matrix(
    c(21, 3, 9, 75, 31, 17, 582, 273, 266),
    nrow=3, 
    ncol=3, 
    byrow=TRUE
)



png(filename="/home/ricardo/Documents/mygraph.png",  width=1200, height =700, res=100)

colnames(data)=c("I", "II", "III")
rownames(data)=c("inconsistent checkpoints","useful checkpoints/number of rollbacks","unreachable checkpoints")
 
# Get the stacked barplot
barplot(data, density=c(20,20,20) , angle=c(0,45,90) , col="brown", border="brown", space=0.06, font.axis=2, xlab="Execution")

# Grouped barplot
barplot(data, density=c(20,20,20) , angle=c(0,45,90) , col="brown", border="brown", font.axis=2, beside=T, legend=rownames(data), xlab="Execution", ylab="Number of created checkpoints", ylim=c(0, 600), font.lab=2)

dev.off()
