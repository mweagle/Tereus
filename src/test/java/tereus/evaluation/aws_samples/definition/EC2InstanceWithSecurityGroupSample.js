CloudFormationTemplate("EC2InstanceWithSecurityGroupSample")({
  "AWSTemplateFormatVersion": "2010-09-09",
  "Description": "AWS CloudFormation Sample Template EC2InstanceWithSecurityGroupSample: Create an Amazon EC2 instance running the Amazon Linux AMI. The AMI is chosen based on the region in which the stack is run. This example creates an EC2 security group for the instance to give you SSH access. **WARNING** This template creates an Amazon EC2 instance. You will be billed for the AWS resources used if you create a stack from this template.",
  "Parameters": {
    "KeyName": {
      "Description": "Name of an existing EC2 KeyPair to enable SSH access to the instance",
      "Type": "AWS::EC2::KeyPair::KeyName",
      "ConstraintDescription": "must be the name of an existing EC2 KeyPair."
    },
    "InstanceType": {
      "Description": "WebServer EC2 instance type",
      "Type": "String",
      "Default": "m1.small",
      "AllowedValues": [
        "t1.micro",
        "t2.micro",
        "t2.small",
        "t2.medium",
        "m1.small",
        "m1.medium",
        "m1.large",
        "m1.xlarge",
        "m2.xlarge",
        "m2.2xlarge",
        "m2.4xlarge",
        "m3.medium",
        "m3.large",
        "m3.xlarge",
        "m3.2xlarge",
        "c1.medium",
        "c1.xlarge",
        "c3.large",
        "c3.xlarge",
        "c3.2xlarge",
        "c3.4xlarge",
        "c3.8xlarge",
        "c4.large",
        "c4.xlarge",
        "c4.2xlarge",
        "c4.4xlarge",
        "c4.8xlarge",
        "g2.2xlarge",
        "r3.large",
        "r3.xlarge",
        "r3.2xlarge",
        "r3.4xlarge",
        "r3.8xlarge",
        "i2.xlarge",
        "i2.2xlarge",
        "i2.4xlarge",
        "i2.8xlarge",
        "hi1.4xlarge",
        "hs1.8xlarge",
        "cr1.8xlarge",
        "cc2.8xlarge",
        "cg1.4xlarge"
      ],
      "ConstraintDescription": "must be a valid EC2 instance type."
    },
    "SSHLocation": {
      "Description": "The IP address range that can be used to SSH to the EC2 instances",
      "Type": "String",
      "MinLength": "9",
      "MaxLength": "18",
      "Default": "0.0.0.0/0",
      "AllowedPattern": "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})",
      "ConstraintDescription": "must be a valid IP CIDR range of the form x.x.x.x/x."
    }
  },
  "Mappings": {
    "AWSInstanceType2Arch": {
      "t1.micro": {
        "Arch": "PV64"
      },
      "t2.micro": {
        "Arch": "HVM64"
      },
      "t2.small": {
        "Arch": "HVM64"
      },
      "t2.medium": {
        "Arch": "HVM64"
      },
      "m1.small": {
        "Arch": "PV64"
      },
      "m1.medium": {
        "Arch": "PV64"
      },
      "m1.large": {
        "Arch": "PV64"
      },
      "m1.xlarge": {
        "Arch": "PV64"
      },
      "m2.xlarge": {
        "Arch": "PV64"
      },
      "m2.2xlarge": {
        "Arch": "PV64"
      },
      "m2.4xlarge": {
        "Arch": "PV64"
      },
      "m3.medium": {
        "Arch": "HVM64"
      },
      "m3.large": {
        "Arch": "HVM64"
      },
      "m3.xlarge": {
        "Arch": "HVM64"
      },
      "m3.2xlarge": {
        "Arch": "HVM64"
      },
      "c1.medium": {
        "Arch": "PV64"
      },
      "c1.xlarge": {
        "Arch": "PV64"
      },
      "c3.large": {
        "Arch": "HVM64"
      },
      "c3.xlarge": {
        "Arch": "HVM64"
      },
      "c3.2xlarge": {
        "Arch": "HVM64"
      },
      "c3.4xlarge": {
        "Arch": "HVM64"
      },
      "c3.8xlarge": {
        "Arch": "HVM64"
      },
      "c4.large": {
        "Arch": "HVM64"
      },
      "c4.xlarge": {
        "Arch": "HVM64"
      },
      "c4.2xlarge": {
        "Arch": "HVM64"
      },
      "c4.4xlarge": {
        "Arch": "HVM64"
      },
      "c4.8xlarge": {
        "Arch": "HVM64"
      },
      "g2.2xlarge": {
        "Arch": "HVMG2"
      },
      "r3.large": {
        "Arch": "HVM64"
      },
      "r3.xlarge": {
        "Arch": "HVM64"
      },
      "r3.2xlarge": {
        "Arch": "HVM64"
      },
      "r3.4xlarge": {
        "Arch": "HVM64"
      },
      "r3.8xlarge": {
        "Arch": "HVM64"
      },
      "i2.xlarge": {
        "Arch": "HVM64"
      },
      "i2.2xlarge": {
        "Arch": "HVM64"
      },
      "i2.4xlarge": {
        "Arch": "HVM64"
      },
      "i2.8xlarge": {
        "Arch": "HVM64"
      },
      "hi1.4xlarge": {
        "Arch": "HVM64"
      },
      "hs1.8xlarge": {
        "Arch": "HVM64"
      },
      "cr1.8xlarge": {
        "Arch": "HVM64"
      },
      "cc2.8xlarge": {
        "Arch": "HVM64"
      }
    },
    "AWSRegionArch2AMI": {
      "us-east-1": {
        "PV64": "ami-50311038",
        "HVM64": "ami-5231103a",
        "HVMG2": "ami-8c6b40e4"
      },
      "us-west-2": {
        "PV64": "ami-5d79546d",
        "HVM64": "ami-43795473",
        "HVMG2": "ami-abbe919b"
      },
      "us-west-1": {
        "PV64": "ami-eb4fa8af",
        "HVM64": "ami-f74fa8b3",
        "HVMG2": "ami-f31ffeb7"
      },
      "eu-west-1": {
        "PV64": "ami-a71588d0",
        "HVM64": "ami-a51588d2",
        "HVMG2": "ami-d5bc24a2"
      },
      "eu-central-1": {
        "PV64": "ami-ac5c61b1",
        "HVM64": "ami-a25c61bf",
        "HVMG2": "ami-7cd2ef61"
      },
      "ap-northeast-1": {
        "PV64": "ami-8d1df78d",
        "HVM64": "ami-a51df7a5",
        "HVMG2": "ami-6318e863"
      },
      "ap-southeast-1": {
        "PV64": "ami-887041da",
        "HVM64": "ami-5e73420c",
        "HVMG2": "ami-3807376a"
      },
      "ap-southeast-2": {
        "PV64": "ami-bb1e6e81",
        "HVM64": "ami-ad1e6e97",
        "HVMG2": "ami-89790ab3"
      },
      "sa-east-1": {
        "PV64": "ami-29aa1234",
        "HVM64": "ami-27aa123a",
        "HVMG2": "NOT_SUPPORTED"
      },
      "cn-north-1": {
        "PV64": "ami-503aa869",
        "HVM64": "ami-543aa86d",
        "HVMG2": "NOT_SUPPORTED"
      }
    }
  },
  "Resources": {
    "EC2Instance": {
      "Type": "AWS::EC2::Instance",
      "Properties": {
        "InstanceType": {
          "Ref": "InstanceType"
        },
        "SecurityGroups": [
          {
            "Ref": "InstanceSecurityGroup"
          }
        ],
        "KeyName": {
          "Ref": "KeyName"
        },
        "ImageId": {
          "Fn::FindInMap": [
            "AWSRegionArch2AMI",
            {
              "Ref": "AWS::Region"
            },
            {
              "Fn::FindInMap": [
                "AWSInstanceType2Arch",
                {
                  "Ref": "InstanceType"
                },
                "Arch"
              ]
            }
          ]
        }
      }
    },
    "InstanceSecurityGroup": {
      "Type": "AWS::EC2::SecurityGroup",
      "Properties": {
        "GroupDescription": "Enable SSH access via port 22",
        "SecurityGroupIngress": [
          {
            "IpProtocol": "tcp",
            "FromPort": "22",
            "ToPort": "22",
            "CidrIp": {
              "Ref": "SSHLocation"
            }
          }
        ]
      }
    }
  },
  "Outputs": {
    "InstanceId": {
      "Description": "InstanceId of the newly created EC2 instance",
      "Value": {
        "Ref": "EC2Instance"
      }
    },
    "AZ": {
      "Description": "Availability Zone of the newly created EC2 instance",
      "Value": {
        "Fn::GetAtt": [
          "EC2Instance",
          "AvailabilityZone"
        ]
      }
    },
    "PublicDNS": {
      "Description": "Public DNSName of the newly created EC2 instance",
      "Value": {
        "Fn::GetAtt": [
          "EC2Instance",
          "PublicDnsName"
        ]
      }
    },
    "PublicIP": {
      "Description": "Public IP address of the newly created EC2 instance",
      "Value": {
        "Fn::GetAtt": [
          "EC2Instance",
          "PublicIp"
        ]
      }
    }
  }
});
